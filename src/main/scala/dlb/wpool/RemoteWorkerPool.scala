package dlb.wpool

import akka.actor._
import akka.routing.SmallestMailboxRouter
import akka.event.Logging
import dlb.scheduler.tasks._
import com.typesafe.config.ConfigFactory
import reflect.ClassTag
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.cluster.ClusterEvent._

class RemoteWorkerPool[W <: Actor : ClassTag](schedService:String, maxWorkers:Int) extends Actor with ActorLogging {
  val workers = context.actorOf(Props[W].withRouter(SmallestMailboxRouter(maxWorkers)), name = self.path.name+"_workers")

  val cluster = Cluster(context.system)
  var stopRequested = false
  var taskCounter = 0

  // subscribe to cluster changes, MemberUp, re-subscribe when restart
  override def preStart(){
    cluster.subscribe(self, classOf[MemberUp])
    cluster.subscribe(self, classOf[MemberExited])
  }

  override def postStop(){
    cluster.unsubscribe(self)
  }

  def receive = {

    case task:Task =>
      taskCounter+=1
      workers ! task

    case result: TaskResult =>
      log.info(s"${sender.path.elements.drop(2).toList.mkString} => $result")
      taskCounter-=1
      if(stopRequested && taskCounter==0) {
        context.stop(self)
      }

    case state: CurrentClusterState =>
      log.debug(state.toString)
      state.members.filter(_.status == MemberStatus.Up) foreach register

    case MemberExited(me) =>
      if (cluster.selfAddress == me.address) {
        log.warning("RemoteWorkerPool MemberDowned -- No new tasks will be requested and existing tasks will finish.")
        stopRequested = true
      }

    case MemberUp(m) =>
      log.info(m.toString())
      register(m)
  }

  // try to register to all nodes, even though there
  // might not be any frontend on all nodes
  def register(member: Member){
    cluster.system.actorFor(RootActorPath(member.address) / "user" / schedService) ! BackendRegistration
  }
}

trait RemoteWorkerApp {

  def workerServiceName:String

  def schedulerServiceName:String

  def createRemoteWorkerPoolFromParsedArgs[T <: Actor : ClassTag] (port:Option[Int]) {
    port.foreach {sp => System.setProperty("workercluster.akka.remote.netty.port", sp.toString)}
    val cfg = ConfigFactory.load.getConfig("workercluster")
    val system = ActorSystem(cfg.getString("system-name"), cfg)

    val actor = system.actorOf(Props(new RemoteWorkerPool[T]( schedulerServiceName, 3 )), workerServiceName )
    Logging(system, actor).info("System.name => " + system.name + ", and scheduler service name => " + schedulerServiceName)
    Logging(system, actor).info( s"port=${actor.path.address.port.toString} and workerServiceName is $workerServiceName" )
  }
}

package dlb.wpool

import akka.actor._
import akka.routing.SmallestMailboxRouter
import akka.event.Logging
import dlb.scheduler.tasks._
import com.typesafe.config.ConfigFactory
import reflect.ClassTag
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.cluster.ClusterEvent._
import language.postfixOps
import scala.concurrent.duration._

class RemoteWorkerPool[W <: Actor : ClassTag](schedService:String, maxWorkers:Int) extends Actor with ActorLogging {
  val cfg = ConfigFactory.load().getConfig("workercluster")
  val sysName = cfg.getString("system-name")
  val cfgSchedulerAddress = Address("akka", sysName, cfg.getString("scheduler-service.host"), cfg.getString("scheduler-service.port").toInt)
  val workers = context.actorOf(Props[W].withRouter(SmallestMailboxRouter(maxWorkers)), name = self.path.name+"_workers")

  log.info("cfgSchedulerAddr = " + cfgSchedulerAddress)
  val cluster = Cluster(context.system)
  var stopRequested = false
  import context.dispatcher
  val tickTask = cluster.system.scheduler.schedule(15 seconds, 30 seconds, self, "tick")
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
    case "tick" => if(!stopRequested) requestWork()

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
        tickTask.cancel()
      }

    case MemberUp(m) ⇒ register(m)
  }

  // try to register to all nodes, even though there
  // might not be any frontend on all nodes
  def register(member: Member){
    cluster.system.actorFor(RootActorPath(member.address) / "user" / schedService) ! BackendRegistration
  }

  def requestWork(){
    if(!stopRequested) {
      val schedulerActor = cluster.system.actorFor(RootActorPath(cfgSchedulerAddress) / "user" / schedService)
      schedulerActor ! NeedWork
    }
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
    val workerServiceInfoString:String = s"port=${actor.path.address.port.toString} and workerServiceName is $workerServiceName"
    Logging(system, actor).info( workerServiceInfoString )
  }
}

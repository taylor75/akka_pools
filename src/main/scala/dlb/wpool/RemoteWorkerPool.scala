package dlb.wpool

import akka.actor._
import akka.routing.SmallestMailboxRouter
import dlb.scheduler.tasks._
import com.typesafe.config.ConfigFactory
import reflect.ClassTag
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.cluster.ClusterEvent._
import language.postfixOps
import scala.concurrent.duration._

class RemoteWorkerPool[W <: Actor : ClassTag](schedService:String, maxWorkers:Int, roles:Set[String]) extends Actor with ActorLogging {
  val workers = context.actorOf(Props[W].withRouter(SmallestMailboxRouter(maxWorkers)), name = self.path.name+"_workers")

  val cluster = Cluster(context.system)
  var stopRequested = false
  private[this] var taskCounter = 0

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
    if (member.roles.exists(roles.contains)) {
      cluster.system.actorSelection(RootActorPath(member.address) / "user" / schedService) ! BackendRegistration
    }
  }

  def convertActorNameToRole(name:String) = name.toLowerCase
}

trait RemoteWorkerApp {

  def workerServiceName:String

  def schedulerServiceName:String

  def createRemoteWorkerPoolFromParsedArgs[T <: Actor : ClassTag] (port:Option[Int], roles:Set[String]):ActorRef = {
    port.foreach {sp => System.setProperty("workercluster.akka.remote.netty.tcp.port", sp.toString)}

    val cfg = ConfigFactory.parseString("akka.cluster.roles = [" + roles.mkString(", ") + "]")
      .withFallback(ConfigFactory.load.getConfig("workercluster"))

    val system = ActorSystem(cfg.getString("system-name"), cfg)

    system.actorOf(Props[RemoteWorkerPool[T]](new RemoteWorkerPool[T]( schedulerServiceName, 3, roles )), workerServiceName )
  }

  def createRemoteWorkerPoolFromParsedArgs[T <: Actor : ClassTag] (port:Option[Int]):ActorRef = {
    createRemoteWorkerPoolFromParsedArgs(port, Set(schedulerServiceName.toLowerCase))
  }
}

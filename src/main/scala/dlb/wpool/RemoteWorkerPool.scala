package dlb.wpool

import akka.actor._
import akka.routing.SmallestMailboxRouter
import akka.event.Logging
import dlb.scheduler.tasks._
import com.typesafe.config.ConfigFactory
import reflect.ClassTag
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.cluster.ClusterEvent.MemberUp
import dlb.scheduler.tasks.Setup
import akka.cluster.ClusterEvent.CurrentClusterState
import dlb.scheduler.tasks.Expire
import language.postfixOps
import scala.concurrent.duration._

class RemoteWorkerPool[W <: Actor : ClassTag](schedService:String, maxWorkers:Int) extends Actor with ActorLogging {
  val cfg = ConfigFactory.load().getConfig("workercluster")
  val sysName = cfg.getString("system-name")

  println("Address Info " +
    List("akka", sysName, cfg.getString("scheduler-service.host"), cfg.getString("scheduler-service.port").toInt).mkString(", ")
  )

  val cfgSchedulerAddress = Address("akka", sysName, cfg.getString("scheduler-service.host"), cfg.getString("scheduler-service.port").toInt)
  println("cfgSchedulerAddress = " + cfgSchedulerAddress)
  val workers = context.actorOf(Props[W].withRouter(SmallestMailboxRouter(maxWorkers)), name = self.path.name+"_workers")

  val cluster = Cluster(context.system)
  var stopRequested = false

  // subscribe to cluster changes, MemberUp
  // re-subscribe when restart
  override def preStart(){ cluster.subscribe(self, classOf[MemberUp]) }
  override def postStop(){ cluster.unsubscribe(self) }

  import context.dispatcher
  val tickTask = context.system.scheduler.schedule(2 seconds, 2 seconds, self, "tick")

  def receive = {
    case Setup => // Tick Obviates

    case task:Task ⇒ workers ! task

    case "tick" => requestWork()

    case result: TaskResult ⇒
      log.info(sender + "\t" + result)

    case state: CurrentClusterState ⇒
      state.members.filter(_.status == MemberStatus.Up) foreach register

    case Expire =>
      log.warning("RemoteWorkerPool set to expire -- No new tasks will be requested and existing tasks will finish.")
      stopRequested = true

    case MemberUp(m) ⇒ register(m)
  }

  // try to register to all nodes, even though there
  // might not be any frontend on all nodes
  def register(member: Member){
    context.actorFor(RootActorPath(member.address) / "user" / schedService) ! BackendRegistration
  }

  def requestWork(){
    if(!stopRequested) {
      val schedAddress = context.actorFor(RootActorPath(cfgSchedulerAddress) / "user" / schedService)
      log.info("taskSchedulerAddress = " + schedAddress.path)
      schedAddress ! NeedWork
    }
  }
}

trait RemoteWorkerApp {

  def workerServiceName:String

  def schedulerServiceName:String

  def createRemoteWorkerPoolFromParsedArgs[T <: Actor : ClassTag] (port:Option[Int], poolSystemName:String ) {
    port.foreach {sp => System.setProperty("workercluster.akka.remote.netty.port", sp.toString)}
    val cfg = ConfigFactory.load.getConfig("workercluster")
    val system = ActorSystem(cfg.getString("system-name"), cfg)
    val actor = system.actorOf(Props(new RemoteWorkerPool[T]( schedulerServiceName, 3 )), workerServiceName )
    Logging(system, actor) info( s"port=${actor.path.address.port.toString}" )
    Logging(system, actor) info( actor.toString() )
    actor ! Setup
  }
}

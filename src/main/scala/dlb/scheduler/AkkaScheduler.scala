package dlb.scheduler

import akka.actor._
import com.typesafe.config._
import dlb.scheduler.tasks._
import reflect.ClassTag
import dlb.scheduler.tasks.BackendRegistration
import akka.event.Logging
import akka.actor.Terminated
import akka.cluster.ClusterEvent.MemberExited
import akka.cluster.{Member, Cluster}

/*
* User: catayl2
* Date: 10/27/12
* Time: 5:17 PM
*/


trait TaskSchedulerApp  {

  def schedulerServiceName:String

  def createSchedulerFromParsedArgs[T <: Actor : ClassTag](schedulerPort:Option[Int]):(ActorRef, ActorSystem) = {
    schedulerPort.foreach {sp => System.setProperty("workercluster.akka.remote.netty.tcp.port", sp.toString)}

    val cfg = ConfigFactory.parseString("akka.cluster.roles = [" + schedulerServiceName.toLowerCase + "]")
      .withFallback(ConfigFactory.load.getConfig("workercluster"))

    val system = ActorSystem(cfg.getString("system-name"), cfg)

    (system.actorOf(Props[T], name = schedulerServiceName), system)
  }
}

trait TaskScheduler extends Actor with ActorLogging {
  var backends = IndexedSeq.empty[ActorRef]
  var jobCounter = 0
  var stopRequested = false

  val cluster = Cluster(context.system)

  def schedulerReceive : PartialFunction[Any, Unit]
  // subscribe to cluster changes, MemberUp, re-subscribe when restart
  override def preStart(){
    cluster.subscribe(self, classOf[MemberExited])
  }

  override def postStop(){
    cluster.unsubscribe(self)
  }

  def processExitedMember(m:Member) {}

  def clusterReceive:PartialFunction[Any, Unit] = {
    case MemberExited(me) =>
      if (cluster.selfAddress == me.address) {
        log.warning("Member Leaving => " + me.toString())
        stopRequested = true
      }
      processExitedMember(me)

    case BackendRegistration if !backends.contains(sender) ⇒
      context watch sender
      backends = backends :+ sender

    case Terminated(a) =>
      log.info("Terminated member: " + a.toString())
      backends = backends.filterNot(_ == a)
  }

  def receive = clusterReceive orElse schedulerReceive
}

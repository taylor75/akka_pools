package dlb.scheduler

import akka.actor._
import com.typesafe.config._
import dlb.scheduler.tasks._
import reflect.ClassTag
import dlb.scheduler.tasks.BackendRegistration
import akka.event.Logging
import akka.actor.Terminated
import language.postfixOps
import scala.concurrent.duration._
import akka.cluster.ClusterEvent.MemberExited
import akka.cluster.Cluster

/*
* User: catayl2
* Date: 10/27/12
* Time: 5:17 PM
*/


trait TaskSchedulerApp  {

  def schedulerServiceName:String

  def createSchedulerFromParsedArgs[T <: Actor : ClassTag](schedulerPort:Option[Int], roleName:String = schedulerServiceName.toLowerCase()):ActorRef = {
    schedulerPort.foreach {sp => System.setProperty("workercluster.akka.remote.netty.tcp.port", sp.toString)}

    val cfg = ConfigFactory.parseString("akka.cluster.roles = [" + roleName + "]")
      .withFallback(ConfigFactory.load.getConfig("workercluster"))

    val system = ActorSystem(cfg.getString("system-name"), cfg)
    val sActor = system.actorOf(Props[T], name = schedulerServiceName)
    Logging(system, sActor).info(message = s"Started Scheduler Application - waiting for messages schedulerSystem = ${sActor.path} toStr: ${sActor.toString()}")
    sActor
  }
}

trait TaskScheduler extends Actor with ActorLogging {
  var backends = IndexedSeq.empty[ActorRef]
  var jobCounter = 0
  var stopRequested = false

  val cluster = Cluster(context.system)
  import context.dispatcher
  val tickTask = context.system.scheduler.schedule(15 seconds, 30 seconds, self, "tick")

  // subscribe to cluster changes, MemberUp, re-subscribe when restart
  override def preStart(){
    cluster.subscribe(self, classOf[MemberExited])
  }

  override def postStop(){
    cluster.unsubscribe(self)
  }

  def findNextJob:Task

  def receive = {

    case "tick" =>
      if(backends.nonEmpty) {
        jobCounter += 1
        backends(jobCounter % backends.size) ! findNextJob
      } else {
        log.error("No backends discovered for ")
      }

    case MemberExited(me) =>
      if (cluster.selfAddress == me.address) {
        log.warning("RemoteWorkerPool MemberDowned -- No new tasks will be requested and existing tasks will finish.")
        stopRequested = true
        tickTask.cancel()
      }

    case BackendRegistration if !backends.contains(sender) ⇒
      context watch sender
      backends = backends :+ sender

    case Terminated(a) =>
      log.info("Terminated backend member: " + a.toString())
      backends = backends.filterNot(_ == a)
  }
}

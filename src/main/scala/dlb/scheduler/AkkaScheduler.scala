package dlb.scheduler

import akka.actor._
import com.typesafe.config._
import dlb.scheduler.tasks._
import reflect.ClassTag
import dlb.scheduler.tasks.BackendRegistration
import akka.event.Logging
import tasks.JobFailed
import akka.actor.Terminated

/*
* User: catayl2
* Date: 10/27/12
* Time: 5:17 PM
*/


trait TaskSchedulerApp  {

  def schedulerServiceName:String = getClass.getSimpleName

  def createSchedulerFromParsedArgs[T <: Actor : ClassTag](schedulerPort:Option[Int]):ActorRef = {
    schedulerPort.foreach {sp => System.setProperty("workercluster.akka.remote.netty.port", sp.toString)}
    val cfg = ConfigFactory.load.getConfig("workercluster")
    val system = ActorSystem(cfg.getString("system-name"), cfg)
    val sActor = system.actorOf(Props[T], name = schedulerServiceName)
    Logging(system, sActor).info(
      s"Started Scheduler Application - waiting for messages schedulerSystem = ${sActor.path} toStr: ${sActor.toString()}"
    )
    sActor
  }
}

trait TaskScheduler extends Actor with ActorLogging {

  var backends = IndexedSeq.empty[ActorRef]
  var jobCounter = 0

  def findNextJob:Task

  def receive = {

    case NeedWork =>
      val nextTask = findNextJob
      log.info("NextTask: "+ nextTask)
      sender ! nextTask

      case job: Task if backends.isEmpty ⇒
        sender ! JobFailed("Service unavailable, try again later", job)

      case job: Task ⇒
        jobCounter += 1
        backends(jobCounter % backends.size) forward job

      case BackendRegistration if !backends.contains(sender) ⇒
        context watch sender
        backends = backends :+ sender

      case Terminated(a) ⇒
        backends = backends.filterNot(_ == a)
  }
}

package dlb.scheduler

import akka.actor._
import com.typesafe.config._
import dlb.scheduler.tasks._
import akka.event.Logging
import scalapara.{DefaultArg, AppArg, ParsedArgs}
import AppArgsDB._
import reflect.ClassTag

/*
* User: catayl2
* Date: 10/27/12
* Time: 5:17 PM
*/

object AppArgsDB {
  val schedulerName = AppArg("-s","Name designated for the task scheduler / coordinator actor system.")
  val schedulerPort = AppArg("-sp", "The port the Scheduler's actor system will be listening to and to which launchers can bind")
  val wPoolApp = AppArg("-l","Name designated for the task launcher actor system.")
  val wPoolPort = AppArg("-wp", "The port the on which remote worker pool listens for requests")
  val numWorkers = DefaultArg("-w", "The number of workers to which an instance of a launcher will route tasks", "3")
  val schedulerHost = DefaultArg("-sh", "The Host Ip Address the Scheduler is running on", "127.0.0.1")
  val workerPoolId = AppArg("-poolStop", "Stop a specificly named pool '-poolName' or view a menu of current running pools to stop [none, all, or $poolName]")

  val systemName = AppArg("-sn", "General purpose way to refer to the name of an actor system")
  val actorName = AppArg("-an", "General purpose way to refer to the name of an actor")
  val actorPort = AppArg("-ap", "General purpose way to refer to the port of an actor")
  val actorCfg = AppArg("-ac", "General purpose way to refer to the cfg of an actor")
  val sysHost = DefaultArg("-ah", "General purpose way to refer to the host of an actor", "127.0.0.1")
}


trait TaskSchedulerApp  {

  def schedulerSystemName:String

  def createSchedulerFromParsedArgs[T <: Actor : ClassTag](paramArgs:ParsedArgs, schedulerSystemName:String):ActorRef = {
    System.setProperty("taskscheduler.akka.remote.netty.hostname", paramArgs(schedulerHost))
    System.setProperty("taskscheduler.akka.remote.netty.port", paramArgs(schedulerPort))
    System.setProperty("taskscheduler.akka.tick-duration", "3s")

    println("sched sys = " + schedulerSystemName)
    val cfg = ConfigFactory.load.getConfig("taskscheduler")
    val system = ActorSystem(schedulerSystemName, cfg)
    val sActor = system.actorOf(Props[T], name = paramArgs(schedulerName))
    println("Started Scheduler Application - waiting for messages schedulerSystem = " + sActor.path + " toStr: " + sActor.toString())
    sActor
  }
}

trait TaskScheduler extends Actor {
  def findNextJob:Task
  val log = Logging(context.system, this)

  def receive = {
    case NeedWork() =>
      val nextTask = findNextJob
      log.info("NextTask: "+ nextTask)
      sender ! nextTask

    case ExpireRemotePool =>
      context.stop(self)
      context.system.shutdown()
  }
}


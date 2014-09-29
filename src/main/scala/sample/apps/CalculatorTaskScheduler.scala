package sample.apps

import dlb.scheduler.{Tick, TaskScheduler, TaskSchedulerApp}
import dlb.scheduler.tasks.Task
import dlb.wpool.RemoteWorkerApp
import scala.concurrent.forkjoin.ThreadLocalRandom
import akka.cluster.ClusterEvent.{LeaderChanged, MemberExited}
import scala.concurrent.duration._
import akka.cluster.Member


/*
* User: ctaylor
* Date: 6/9/13
* Time: 7:09 PM
*/


object CalculatorTaskScheduler extends TaskSchedulerApp {
  val rnd:java.util.Random = new java.util.Random()

  def schedulerServiceName = "CalculatorTaskScheduler"

  def findNextJob:Add = { Add(rnd.nextInt(25), rnd.nextInt(33)) }

  def main(args: Array[String]) {
    val thePort:Option[Int] = args.headOption.map {_.toInt}
    val (schedulerRef, schedulerSystem) = createSchedulerFromParsedArgs[CalculatorTaskScheduler]( thePort )
  }
}

class CalculatorTaskScheduler extends TaskScheduler {
	import scala.concurrent.ExecutionContext.Implicits.global

  override def preStart() {
    super.preStart()
    cluster.subscribe(self, classOf[LeaderChanged])
		context.system.scheduler.schedule(5 seconds, 30 seconds, self, Tick)
    println("In CalculatorTaskScheduler.preStart()")
  }

  override def processExitedMember(m:Member){
    if(cluster.selfAddress == m.address) context.stop(self)
  }

  def schedulerReceive = {

    case ldrChanged:LeaderChanged =>
      log.warning("LeaderChanged => " + ldrChanged)

		case Tick =>
			if(backends.nonEmpty) {
				(0 to 15).foreach{i =>
					backends(ThreadLocalRandom.current.nextInt(backends.size)) ! CalculatorTaskScheduler.findNextJob
				}
			}

    case other => log.warning(s"unknown msg in CalculatorTaskScheduler - $other")
  }

}

object RemoteCalculatorPoolApp extends RemoteWorkerApp {
  def workerServiceName = "RemoteCalculatorPoolApp"

  def schedulerServiceName:String = CalculatorTaskScheduler.schedulerServiceName

  def main(args: Array[String]) {
    createRemoteWorkerPoolFromParsedArgs[AddActor]( args.headOption.map {_.toInt} )
  }
}

object MultiplyTaskScheduler extends TaskSchedulerApp {
  val rnd = new scala.util.Random()

  def schedulerServiceName = "MultiplyTaskScheduler"

  def findNextJob:Task = {
    Mult(rnd.nextInt(19), rnd.nextInt(19))
  }

  def main(args: Array[String]) {
    val thePort:Option[Int] = args.headOption.map {_.toInt}
    val (schedulerRef, schedulerSystem) = createSchedulerFromParsedArgs[MultiplyTaskScheduler]( thePort, Set("multiply") )

    Thread.sleep(5000)
    (0 to 2000).foreach {i =>
      schedulerRef ! findNextJob
      Thread.sleep(2000)
    }

    schedulerSystem.shutdown()
  }
}

class MultiplyTaskScheduler extends TaskScheduler {

  def schedulerReceive = {
    case m:Mult =>
      if(backends.nonEmpty) {
        backends(ThreadLocalRandom.current.nextInt(backends.size)) ! m
      } else {
        log.error(s"No backends discovered for $m")
      }
  }
}

object RemoteMultiplyPoolApp extends RemoteWorkerApp {
  def workerServiceName = "RemoteMultiplyPoolApp"

  def schedulerServiceName:String = MultiplyTaskScheduler.schedulerServiceName

  def main(args: Array[String]) {
    val myPort = args.headOption.map {hd => hd.toInt}
    createRemoteWorkerPoolFromParsedArgs[MultiplyActor]( myPort, Set("multiply") )
  }
}

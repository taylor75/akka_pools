package sample.apps

import dlb.scheduler.{TaskScheduler, TaskSchedulerApp}
import dlb.scheduler.tasks.Task
import dlb.wpool.RemoteWorkerApp

/*
* User: ctaylor
* Date: 6/9/13
* Time: 7:09 PM
*/


object CalculatorTaskScheduler extends TaskSchedulerApp {

  def schedulerServiceName = "CalculatorTaskScheduler"

  def main(args: Array[String]) {
    val thePort:Option[Int] = args.headOption.map {_.toInt}
    createSchedulerFromParsedArgs[CalculatorTaskScheduler]( thePort )
  }
}


class CalculatorTaskScheduler extends TaskScheduler {
  val rnd:java.util.Random = new java.util.Random()

  def findNextJob:Task = {
    if (rnd.nextBoolean())
      Add(rnd.nextInt(25), rnd.nextInt(33))
    else Add(java.lang.Math.max(30, rnd.nextInt(90)), -1*rnd.nextInt(30))
  }

}

object RemoteCalculatorPoolApp extends RemoteWorkerApp {
  def workerServiceName = "RemoteCalculatorPoolApp"

  def schedulerServiceName:String = CalculatorTaskScheduler.schedulerServiceName

  def main(args: Array[String]) {
    createRemoteWorkerPoolFromParsedArgs[AddSubtractActor]( args.headOption.map {_.toInt} )
  }
}

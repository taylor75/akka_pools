package sample.remote.calculator

import scalapara.CmdLineApp
import dlb.scheduler._
import dlb.scheduler.tasks._
import dlb.wpool.RemoteWorkerApp
import dlb.scheduler.AppArgsDB._

class CalculatorTaskScheduler extends TaskScheduler {
  val rnd:java.util.Random = new java.util.Random()

  def findNextJob:Task = {
    if (rnd.nextBoolean())
      Add(rnd.nextInt(25), rnd.nextInt(33))
    else Add(java.lang.Math.max(30, rnd.nextInt(90)), -1*rnd.nextInt(30))
  }

}

object RemoteCalculatorPoolApp extends CmdLineApp("RemoteCalculatorPoolApp", Array(port)) with RemoteWorkerApp {

  def workerServiceName:String = appName

  def schedulerServiceName:String = CalculatorTaskScheduler.schedulerServiceName

  override def description:String = "An CmdLineApp for bringing online a distributed pool of workers supervised by a RemoteWorkerPool Actor."

  def main(args: Array[String]) {
    parseAndValidateParamArgs(args) foreach{ appArgs =>
      val parsedPort = appArgs(port).toInt
      val sPort = if(parsedPort == 0) None else Some(parsedPort)
      createRemoteWorkerPoolFromParsedArgs[AddSubtractActor](sPort, workerServiceName)
    }
  }
}

object CalculatorTaskScheduler extends CmdLineApp("CalculatorTaskScheduler", Array(port)) with TaskSchedulerApp {

  def schedulerServiceName = appName

  override def description =
    "An executable for bringing online a master task scheduler to which pools of workers supervised by launchers will communicate."

  def main(args: Array[String]) {
    parseAndValidateParamArgs(args) foreach{ paramArgs =>
      val parsedPort = paramArgs(port).toInt
      val sPort = if(parsedPort == 0) None else Some(parsedPort)

      createSchedulerFromParsedArgs[CalculatorTaskScheduler]( sPort )
    }
  }
}

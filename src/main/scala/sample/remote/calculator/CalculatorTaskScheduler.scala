package sample.remote.calculator

import scalapara.{CmdLineApp, CmdLineAppSuite}
import dlb.scheduler._
import dlb.scheduler.tasks._
import dlb.wpool.RemoteWorkerApp
import dlb.shutdown.ShutdownWorkers
import dlb.scheduler.AppArgsDB._

class CalculatorTaskScheduler extends TaskScheduler {
  val rnd:java.util.Random = new java.util.Random()

  def findNextJob:Task = {
    if (rnd.nextBoolean())
      Add(rnd.nextInt(25), rnd.nextInt(33))
    else Add(java.lang.Math.max(30, rnd.nextInt(90)), -1*rnd.nextInt(30))
  }

}

object RemoteCalculatorPoolApp
  extends CmdLineApp("RemoteCalculatorPoolApp", Array(sysHost, wPoolApp, wPoolPort, numWorkers, schedulerName, schedulerHost, schedulerPort)) with RemoteWorkerApp {

  override def description:String = "An CmdLineApp for bringing online a distributed pool of workers supervised by a RemoteWorkerPool Actor."

  def main(args: Array[String]) {
    parseAndValidateParamArgs(args) foreach{ appArgs =>
      createRemoteWorkerPoolFromParsedArgs[AddSubtractActor](appArgs, appName, CalculatorTaskScheduler.appName)
    }
  }
}

object CalculatorTaskScheduler
  extends CmdLineApp("CalculatorTaskScheduler", Array(schedulerName, schedulerHost, schedulerPort)) with TaskSchedulerApp {

  def schedulerSystemName = appName

  override def description =
    "An executable for bringing online a master task scheduler to which pools of workers supervised by launchers will communicate."

  def main(args: Array[String]) {
    parseAndValidateParamArgs(args) foreach{ paramArgs =>
      createSchedulerFromParsedArgs[CalculatorTaskScheduler](paramArgs, appName)
    }
  }

}

object RemoteCalculatorAppSuite extends CmdLineAppSuite("RemoteCalculatorAppSuite", List(CalculatorTaskScheduler, RemoteCalculatorPoolApp, ShutdownWorkers)) {

  def selectApp(args:Array[String]):Option[CmdLineApp] = {
    for {
      appNmArg <- args.headOption
      chosenApp <- cmdLineApps.find{anApp:CmdLineApp =>  appNmArg.equals(anApp.appName)}
    }
    yield chosenApp
  }

  def main (args:Array[String]) {
    selectApp(args) match {
        case None => printInfo
        case Some(appChoice) => appChoice.main(args.tail)
      }
  }
}

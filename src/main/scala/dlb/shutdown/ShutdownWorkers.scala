package dlb.shutdown

import akka.actor.{Props, Actor, ActorSystem}
import com.typesafe.config.ConfigFactory
import akka.remote.RemoteClientShutdown
import akka.event.Logging
import dlb.scheduler.tasks._
import scalapara.CmdLineApp
import dlb.scheduler.AppArgsDB._

object ShutdownWorkers extends CmdLineApp("ShutdownWorkerPool", Array(sysHost, systemName, actorName, actorPort, actorCfg)) {
  override def description = "An App to Gracefully Shutdown actor systems running on the current host"

  def main(args: Array[String]) {

    parseAndValidateParamArgs(args) foreach{ appArgs =>
      val cfg = ConfigFactory.load.getConfig(appArgs(actorCfg))
      val hostName = appArgs(sysHost)

      val system = ActorSystem(appArgs(systemName), cfg)
      val greeter = system.actorOf(Props(new ShutdownActor(List("akka://"+appArgs(systemName)+"@"+hostName+":" + appArgs(actorPort) + "/user/"+appArgs(actorName)))), name = "destroya")

      greeter ! Setup
      println("Just sent off Setup Msg")
      system.eventStream.subscribe(greeter, classOf[RemoteClientShutdown])
    }
  }
}

class ShutdownActor(paths:List[String]) extends Actor {
  println("paths = " + paths.mkString(", "))
  val workerActorRefs = paths.map{context.actorFor(_)}
  var deathWatchCount = workerActorRefs.size
  val log = Logging(context.system, this)

  def receive = {
    case Setup =>
      println("Received Setup Msg")
      log.debug("Trying to exterminate -- " + workerActorRefs.map{_.path}.mkString("\n"))
      workerActorRefs.foreach {_ ! ExpireRemotePool }

    case r:RemoteClientShutdown =>
      log.info("RemoteShutdown detected! [" + deathWatchCount + "] for " + r)
      deathWatchCount -= 1

      if (deathWatchCount == 0) {
        context.stop(self)
        context.system.shutdown()
      }
  }
}

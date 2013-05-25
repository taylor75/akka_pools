package dlb.wpool

import akka.actor.{ActorSystem, Props, Actor}
import akka.routing.SmallestMailboxRouter
import akka.event.Logging
import java.net.InetAddress
import dlb.scheduler.tasks._
import com.typesafe.config.ConfigFactory
import scalapara.ParsedArgs
import dlb.scheduler.AppArgsDB._

class RemoteWorkerPool[W <: Actor : Manifest](schedSysName:String, schedName:String, schedHost:String, schedPort:Int, maxWorkers:Int, wPoolPort:Int) extends Actor {
  val schedulerPath = "akka://"+schedSysName+"@"+schedHost+":"+schedPort.toString+"/user/"+schedName
  val taskScheduler = context.actorFor(schedulerPath)
  val workers = context.actorOf(Props[W].withRouter(SmallestMailboxRouter(maxWorkers)), name = self.path.name+"_workers")
  val log = Logging(context.system, this)

  var stopRequested = false
  var currentWorkerCount = 0

  def receive = {
    case Setup =>
      requestWork()

    case (task: Task) ⇒
      log.debug("task=" + task + " [" + currentWorkerCount + "/" + maxWorkers + "]")
      workers ! task
      requestWork()

    case result: TaskResult ⇒
      currentWorkerCount -= 1
      log.info(sender + "\t" + result +"\t" + currentWorkerCount)
      requestWork()

    case ExpireRemotePool =>
      log.warning("RemoteWorkerPool set to expire -- No new tasks will be requested and existing tasks ["+currentWorkerCount+"] will finish.")
      stopRequested = true

    case e@TaskExecutionError(error: Throwable) =>
      currentWorkerCount -= 1
      log.error(List("TaskExecutionError: ", e, " workerCount=", currentWorkerCount).mkString(""))
      requestWork()
  }

  def requestWork(){
    if(!stopRequested) {
      if (currentWorkerCount < maxWorkers) {
        currentWorkerCount += 1
        taskScheduler ! NeedWork()
      }
    }
    else if (currentWorkerCount == 0) {
      log.warning("********* Launcher System Shutting Down *********** ["+self.path.name+"]")
      context.stop(self)
      context.system.shutdown()
    }
  }
}

trait RemoteWorkerApp {

  def createRemoteWorkerPoolFromParsedArgs[T <: Actor : Manifest] (parsedArgs:ParsedArgs, poolSystemName:String, schedulerSystemName:String) {

    System.setProperty("workercluster.akka.remote.netty.hostname", InetAddress.getLocalHost.getHostAddress)
    System.setProperty("workercluster.akka.remote.netty.port", parsedArgs(wPoolPort))

    val cfg = ConfigFactory.load.getConfig("workercluster")
    val system = ActorSystem(poolSystemName, cfg)

    val (lName, nWorkers, lPort, sPort, sName, sHost) = (
      parsedArgs(wPoolApp), parsedArgs(numWorkers).toInt, parsedArgs(wPoolPort).toInt, parsedArgs(schedulerPort),
      parsedArgs(schedulerName), parsedArgs(schedulerHost))

    val actor = system.actorOf(Props(new RemoteWorkerPool[T]( schedulerSystemName, sName, sHost, sPort.toInt, nWorkers, lPort)), name = lName )

    Logging(system, actor).info(actor.toString())
    actor ! Setup
  }
}

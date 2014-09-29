package sample.akka

import akka.actor.{ActorSystem, Props, Actor, ActorLogging}
import scala.concurrent.forkjoin.ThreadLocalRandom
import akka.pattern.{ask, pipe}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import akka.util.Timeout

/*
* User: catayl2
* Date: 6/29/13
* Time: 11:41 PM
*/

case object DoWork
case class SomeWork(i:Int)
case class WorkResponse(i:Int)
case class AllResponses(answers:List[WorkResponse])

object TestApp extends App {
  implicit val timeout = Timeout(15 seconds)
  val mySystem = ActorSystem("TestSystem")
  val contentMgr = mySystem.actorOf(Props[ContentMgr], "ContentMgr")
  val aggregatorResponse = Await.result(contentMgr ask DoWork, 1 minute)

  println(s"Aggregator answer to all of life's problems is $aggregatorResponse")

}

class ContentMgr extends Actor with ActorLogging {
  implicit val timeout = Timeout(60.seconds)
  import scala.concurrent.ExecutionContext.Implicits.global

  val aggregatorRef = context.actorOf(Props[Aggregator], "Aggregator")
  val doublerRef = context.actorOf(Props[DoublerWorker], "Doubler")
  val triplerRef = context.actorOf(Props[TriplingWorker], "Tripler")
  val workers = List(doublerRef,triplerRef)

  def receive = {
    case DoWork =>
      val someWork = SomeWork(ThreadLocalRandom.current().nextInt(5) + 1)
      println(s"Sending out: $someWork")

      Future.sequence(workers.map{wkr =>
        (wkr ask someWork).mapTo[WorkResponse]
      }).map{responses =>
        Aggregator.calculate(AllResponses(responses))
      } pipeTo sender
  }
}

class Aggregator extends Actor with ActorLogging {

  def receive = {
    case responseList:AllResponses =>
      val sum = Aggregator.calculate(responseList)
      println(s"The sum of all responses from workers is $sum")
      sender ! sum
  }
}
object Aggregator {
  def calculate(allResp:AllResponses):Int = {
    allResp.answers.map{_.i}.sum
  }
}

class DoublerWorker extends Actor with ActorLogging {
  def receive = {
    case SomeWork(intVal) =>
      Thread.sleep(ThreadLocalRandom.current().nextInt(5) * 2000)
      val answer = WorkResponse(intVal*2)
      println(s"Doubler is finished $answer")
      sender ! answer
  }
}

class TriplingWorker extends Actor with ActorLogging {
  def receive = {
    case SomeWork(intVal) =>
      Thread.sleep(ThreadLocalRandom.current().nextInt(5) * 2000)
      val answer = WorkResponse(intVal*3)
      println(s"TriplingWorker is finished $answer")
      sender ! answer
  }
}


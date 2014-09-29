package sample.akka

import akka.actor.{ActorSystem, Props, Actor, ActorLogging}
import akka.cluster.Cluster
import akka.contrib.pattern.DistributedPubSubMediator.{SubscribeAck, Publish, Subscribe}
import com.typesafe.config.ConfigFactory
import language.postfixOps
import scala.concurrent.duration._
import akka.cluster.ClusterEvent.MemberExited
import akka.pattern.ask
import scala.concurrent.{Await, Future}
import akka.util.Timeout

/*
* User: ctaylor
* Date: 7/8/13
* Time: 1:03 PM
*/

object Tick

class PubSubPublisherReceiverTest extends Actor with ActorLogging {
  import akka.contrib.pattern.DistributedPubSubExtension
  import scala.concurrent.ExecutionContext.Implicits.global

  val tickTask = context.system.scheduler.schedule(30 seconds, 2 minutes, self, Tick)
  implicit val timeout = Timeout(9.seconds)
  val cluster = Cluster(context.system)
  lazy val pubSubMediator = DistributedPubSubExtension(cluster.system).mediator

  override def preStart(){
    cluster.subscribe(self, classOf[MemberExited])
  }

  override def postStop(){
    cluster.unsubscribe(self)
  }

  def receive = {

    case Tick =>
      println("Received Tick in Publisher")
      val msgToSend = MsgFromPublisher("Hi Subscriber, I need you to Ack somebody! "+System.currentTimeMillis().toString)
      val future:Future[AckFromSubscriber] = ask(pubSubMediator, Publish("replyTest", msgToSend)).mapTo[AckFromSubscriber]
        Await.result(future, 5 seconds) match {
          case ack:AckFromSubscriber => log.info("the ack from subscriber: " + ack)
          case unknown => log.info("Unknown from Subscriber")
        }

    case other => log.info("other in Publisher: " + other)
  }
}

class PubSubSubscriberReplyTest extends Actor with ActorLogging {
  import scala.concurrent.ExecutionContext.Implicits.global
  import akka.contrib.pattern.DistributedPubSubExtension

  val cluster = Cluster(context.system)

  lazy val pubSubMediator = DistributedPubSubExtension(cluster.system).mediator

  pubSubMediator ! Subscribe("replyTest", self)

  override def preStart(){
    cluster.subscribe(self, classOf[MemberExited])
  }

  override def postStop(){
    cluster.unsubscribe(self)
  }

  def receive = {
    case SubscribeAck(Subscribe(pubSubTopic, None, `self`)) => log.info("SubscribeAck for " + pubSubTopic)

    case msg:MsgFromPublisher =>
      log.info("Received msg from Subscriber, sending back: "+ AckFromSubscriber("I got this from you: " + msg))
      sender ! AckFromSubscriber("I got this from you: " + msg)

    case other => log.info("other Subscriber: " + other)
  }
}

case class MsgFromPublisher(msg:String)
case class AckFromSubscriber(msg:String)

object PublisherTestApp extends App {
  System.setProperty("workercluster.akka.remote.netty.tcp.port", args(0).toString)

  val cfg = ConfigFactory.parseString("akka.cluster.roles = [pubSubAck]")
    .withFallback(ConfigFactory.load.getConfig("workercluster"))
  println("SysName=" +cfg.getString("system-name"))
  val system = ActorSystem(cfg.getString("system-name"), cfg)

  val pubActor = system.actorOf(Props(new PubSubPublisherReceiverTest),
    name = "PublisherActor")
}

object SubscriberTestApp extends App {
  System.setProperty("workercluster.akka.remote.netty.tcp.port", args(0).toString)

  val cfg = ConfigFactory.parseString("akka.cluster.roles = [pubSubAck]")
    .withFallback(ConfigFactory.load.getConfig("workercluster"))

  val system = ActorSystem(cfg.getString("system-name"), cfg)

  val pubActor = system.actorOf(Props(new PubSubSubscriberReplyTest),
    name = "SubscriberActor")
}


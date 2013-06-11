package sample.apps

import akka.actor.{ActorLogging, Actor}
import dlb.scheduler.tasks._

/*
* User: catayl2
* Date: 7/7/12
* Time: 11:39 AM
*/

class AddSubtractActor extends Actor with ActorLogging {
  def receive = {
    case t:Task ⇒ t match {
      case Add(n1, n2) =>  sender ! TaskComplete(AddResult(n1, n2, n1 + n2), 0d)
      case other => log.error(t + " was not supposed to be sent to AddSubtractActor")
    }
  }
}

class MultiplyActor extends Actor with ActorLogging {
  def receive = {
    case t:Task ⇒ t match {
      case m:Mult =>  sender ! TaskComplete(MultResult(m, (m.nbr1 * m.nbr2)), 0d)
      case other => log.error(t + " was not supposed to be sent to AddSubtractActor")
    }
  }
}

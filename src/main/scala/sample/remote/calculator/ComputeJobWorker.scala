package sample.remote.calculator

import akka.actor.Actor
import dlb.scheduler.tasks._

/*
* User: catayl2
* Date: 7/7/12
* Time: 11:39 AM
*/

class AddSubtractActor extends Actor {
  def receive = {
    case t:Task ⇒ t match {
      case Add(n1, n2) => try{
        sender ! TaskComplete(AddResult(n1, n2, n1 + n2), 0d)
      } catch {
        case e:Throwable => sender ! TaskExecutionError(e)
      }
    }
  }
}

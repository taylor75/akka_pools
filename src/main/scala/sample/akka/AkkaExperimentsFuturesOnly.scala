package sample.akka

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import akka.util.Timeout
import scala.concurrent.forkjoin.ThreadLocalRandom

/*
* User: catayl2
* Date: 6/30/13
* Time: 12:58 PM
*/

object AkkaExperimentsFuturesOnly extends App {
  import scala.concurrent.ExecutionContext.Implicits.global

  // Just local vals
  val doubler = new Analyzer {
    def doSomeWork(someWork: SomeWork) = WorkResponse(someWork.i * 2)
  }

  val tripler = new Analyzer {
    def doSomeWork(someWork: SomeWork) = WorkResponse(someWork.i * 3)
  }

  val reducer = new Reducer {
    def reduceFunc = new Function1[AllResponses,WorkResponse]{
      def apply(v1: AllResponses) = WorkResponse(v1.answers.map{_.i}.sum)
    }
  }

  val workers = List(doubler, tripler)
  (0 to 15).foreach {i =>
    val someWork = SomeWork(i)
    println(someWork)

    val sequencedFuture = Future.sequence(workers.map{wkr =>
      Future{ wkr.doSomeWork(someWork)}.mapTo[WorkResponse]
    }).flatMap{responses =>
      Future { reducer.reduceResponses(AllResponses(responses)) }
    }

    println(Await.result(sequencedFuture, 5 seconds))
    println()
  }

}

trait Analyzer {
  def doSomeWork(someWork:SomeWork):WorkResponse
}

trait Reducer {
  def reduceResponses(allResponses:AllResponses):WorkResponse = {
    reduceFunc(allResponses)
  }

  def reduceFunc:(AllResponses) => WorkResponse
}

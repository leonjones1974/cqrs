package uk.camsw.cqrs

import java.util.concurrent.Executors
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

trait ConcurrencyTestSupport {
  def asyncAndWait(action: => Future[_], times: Int = 3) {
    implicit val publisherContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
    val futures = for {
      n <- (1 to times).toList
      f = Future {
        action
      }
    } yield f flatMap identity

    Await.ready(Future.sequence(futures), 1 second)
  }

}

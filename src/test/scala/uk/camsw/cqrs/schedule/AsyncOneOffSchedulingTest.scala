package uk.camsw.cqrs.schedule

import java.util.concurrent.Executors

import com.google.common.util.concurrent.MoreExecutors
import org.joda.time.Duration._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import uk.camsw.cqrs._
import uk.camsw.cqrs.schedule.Commands.{InvokeNow, ScheduleDomainCommand}
import uk.camsw.cqrs.test.DomainTestSupport

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, TimeoutException}
import scala.reflect._

class AsyncOneOffSchedulingTest extends FunSpec with BeforeAndAfter with Matchers
  with DomainTestSupport[ScheduleDomainCommand, ScheduleDomain]
  with ScheduleTestSupport
  with MockFactory {

  override val executionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))
  override val commandTag = classTag[ScheduleDomainCommand]

  var oneOffTask: TestTask = _
  var scheduleDomain: ScheduleDomain = _
  var domainBus: EventBus = _

  implicit val testEc = ExecutionContext.fromExecutor(MoreExecutors.directExecutor())

  describe("Multi-threaded scheduling - Ask") {
    it("should return a future") {
      val f = domain ? InvokeNow(aTestTask(freq = standardSeconds(1), executionTime = standardSeconds(1)).task)
      await[Any](f)()
    }

    it("should return a future only when the task is complete") {

      val f = domain ? InvokeNow(aTestTask(freq = standardSeconds(1), executionTime = standardSeconds(2)).task)
      intercept[TimeoutException]{await[Any](f)(0.5 seconds)}
    }
  }

  //todo: Pull this out somewhere
  def await[A](f: Future[Option[Event[_]]])(wait: Duration = 3 seconds): A = {
    Await.result(f map (_.get.data.asInstanceOf[A]), wait)
  }

  override def domainUnderTest(): Domain[ScheduleDomainCommand, ScheduleDomain] = {
    domainBus = EventBus(ExecutionContext.fromExecutor(MoreExecutors.directExecutor()))
    scheduleDomain = ScheduleDomain()(domainBus)
    scheduleDomain
  }

}

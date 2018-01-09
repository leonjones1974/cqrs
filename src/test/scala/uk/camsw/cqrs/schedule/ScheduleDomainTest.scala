package uk.camsw.cqrs.schedule

import com.google.common.util.concurrent.MoreExecutors
import org.joda.time.Duration.standardSeconds
import org.joda.time.Instant
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import uk.camsw.cqrs._
import uk.camsw.cqrs.schedule.Commands._
import uk.camsw.cqrs.test.DomainTestSupport

import scala.concurrent.ExecutionContext
import scala.reflect.classTag

class ScheduleDomainTest extends FunSpec with BeforeAndAfter with Matchers
  with DomainTestSupport[ScheduleDomainCommand, ScheduleDomain]
  with ScheduleTestSupport
  with MockFactory {

  import ScheduleDomainTest._

  override val commandTag = classTag[ScheduleDomainCommand]

  var everySecond, everyOtherSecond, oneOffTask: TestTask = _
  var scheduleDomain: ScheduleDomain = _
  var domainBus: EventBus = _

  describe("The Schedule Domain") {

    it("should invoke tasks upon initial clock tick") {
      domain << invokeTasks(startTime)
      everySecond.invocations shouldBe 1
    }

    it("should invoke all tasks on the same schedule, upon initial tick") {
      domain << invokeTasks(startTime)
      everyOtherSecond.invocations shouldBe 1
    }

    it("should invoke tasks when due, following initial tick") {
      domain << invokeTasks(startTime)

      domain << invokeTasks(startTime plus 999)
      everySecond.invocations shouldBe 1
      everyOtherSecond.invocations shouldBe 1

      domain << invokeTasks(startTime plus standardSeconds(1))
      everySecond.invocations shouldBe 2
      everyOtherSecond.invocations shouldBe 1

      domain << invokeTasks(startTime plus 1001)
      everySecond.invocations shouldBe 2
      everyOtherSecond.invocations shouldBe 1

      domain << invokeTasks(startTime plus standardSeconds(2))
      everySecond.invocations shouldBe 3
      everyOtherSecond.invocations shouldBe 2
    }

    it("should support immediate invocation of a task") {
      domain << InvokeNow(oneOffTask.task)
      oneOffTask.invocations shouldBe 1
    }

  }

  override def domainUnderTest(): Domain[ScheduleDomainCommand, ScheduleDomain] = {
    everySecond = aTestTask(standardSeconds(1))
    everyOtherSecond = aTestTask(standardSeconds(2))
    oneOffTask = aTestTask(standardSeconds(0))
    domainBus = EventBus(ExecutionContext.fromExecutor(MoreExecutors.directExecutor()))
    scheduleDomain = ScheduleDomain(
      everySecond.task,
      everyOtherSecond.task
    )(domainBus)
    scheduleDomain
  }
}

object ScheduleDomainTest extends ScheduleTestSupport {
  val startTime = Instant.now()
  val invokeTasks: Instant => Command[ScheduleDomainCommand] = now => InvokeDue(now)
  val successfulInvocationResult = aSuccessfulInvocationResult()
  val failedInvocationResult = aFailedInvocationResult()
}

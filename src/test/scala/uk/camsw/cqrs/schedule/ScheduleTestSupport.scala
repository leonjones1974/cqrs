package uk.camsw.cqrs.schedule

import com.github.nscala_time.time.StaticDuration
import grizzled.slf4j.Logging
import org.joda.time.Duration
import uk.camsw.cqrs.schedule.Models.{InvocationResult, Task}
import uk.camsw.cqrs.test.CqrsDataSupport

import scala.util.{Failure, Success}

trait ScheduleTestSupport extends CqrsDataSupport with Logging {

  def aTaskId = aString
  def aTestTask(freq: Duration, executionTime: Duration = StaticDuration.millis(1)) = new TestTask(freq, executionTime)

  val aFailingTask: Duration => Task[_] = freq => Task(freq, () => {
    Thread.sleep(2)
    Failure(new RuntimeException("Test task failed"))
  }, aTaskId)

  def aSuccessfulInvocationResult() = {
    val start = anInstant
    InvocationResult(aTestTask(aDuration, aDuration).task, start, aDuration, Success(aString))
  }

  def aFailedInvocationResult() = {
    val start = anInstant
    InvocationResult(aFailingTask(aDuration), start, aDuration, Failure(new RuntimeException(aString)))
  }
}

class TestTask(freq: Duration, wait: Duration, var invocations: Int = 0) extends ScheduleTestSupport {
  val task = Task(freq, () => {
    debug(s"Executing task with duration: $freq and wait: $wait")
    invocations = invocations + 1
    Thread.sleep(wait.getMillis)
    Success(s"Test task invoked: [$invocations]")
  }, aTaskId)
}

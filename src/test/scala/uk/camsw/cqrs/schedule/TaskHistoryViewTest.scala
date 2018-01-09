package uk.camsw.cqrs.schedule

import java.util.UUID

import org.scalatest.{FunSpec, Matchers}
import uk.camsw.cqrs.schedule.Events.TaskCompleted
import uk.camsw.cqrs.schedule.Queries.{QueryTaskHistory, TaskHistoryQueried, TaskInvocationRepresentation}
import uk.camsw.cqrs.schedule.ScheduleDomainTest._
import uk.camsw.cqrs.test.ViewTestSupport

class TaskHistoryViewTest extends FunSpec with Matchers
  with ViewTestSupport[QueryTaskHistory, TaskHistoryView] {
  override val queryTag = reflect.classTag[QueryTaskHistory]

  val MaxHistory = 50
  override def viewUnderTest() = TaskHistoryView(maxHistory = MaxHistory)

  import TaskHistoryViewTest._

  describe("Task history view") {

    it("should return successful results, most recent first") {
      val taskInvoked = taskInvokedSuccessfully
      view << taskInvoked
      view << taskInvoked
      view << Queries.All
      view.raisedEvents.head match {
        case TaskHistoryQueried(data) =>
          data.size shouldBe 2
          data.head shouldBe TaskInvocationRepresentation(successfulInvocationResult)
          data(1) shouldBe TaskInvocationRepresentation(successfulInvocationResult)
      }
    }

    it("should include unsuccessful results") {
      view << taskInvokedUnsuccessfully
      view << taskInvokedSuccessfully
      view << Queries.All
      view.raisedEvents.head match {
        case TaskHistoryQueried(data) =>
          data.size shouldBe 2
          data.head.success shouldBe true
          data(1).success shouldBe false
      }
    }

    it("should return empty for query all invocations initially") {
      view << Queries.All
      view.raisedEvents should contain only TaskHistoryQueried(List())(anyId)
    }

    it("should limit task history according to max size") {
      val taskInvoked = taskInvokedSuccessfully
      for {n <- 1 to 100} view << taskInvoked
      view << Queries.All
      view.raisedEvents.head match {
        case TaskHistoryQueried(data) => data.size shouldBe MaxHistory
      }
    }
  }
}

object TaskHistoryViewTest {
  val successfulInvocationResult = aSuccessfulInvocationResult()
  val failedInvocationResult = aFailedInvocationResult()

  def taskInvokedSuccessfully = TaskCompleted(successfulInvocationResult, UUID.randomUUID())
  def taskInvokedUnsuccessfully = TaskCompleted(failedInvocationResult, UUID.randomUUID())
}

package uk.camsw.cqrs.schedule

import java.util.UUID

import uk.camsw.cqrs.schedule.Models.{InvocationResult, TaskId}
import uk.camsw.cqrs.{QueryCommand, ViewQueried}

object Queries {

  case class TaskInvocationRepresentation(id: TaskId, startTimeMs: Long, elapsedTimeMs: Long, message: String, success: Boolean)
  case class TaskHistoryQueried(data: List[TaskInvocationRepresentation])(override val id: UUID) extends ViewQueried[TaskInvocationRepresentation]

  object TaskInvocationRepresentation {
    def apply: InvocationResult => TaskInvocationRepresentation = r =>
      TaskInvocationRepresentation(r.task.id, r.startTime.getMillis, r.elapsedTime.getMillis, r.message, r.result.isSuccess)
  }

  trait QueryTaskHistory extends QueryCommand {
    def execute(view: TaskHistoryView, id: UUID): TaskHistoryQueried
  }

  val All = new QueryTaskHistory {
    def execute(view: TaskHistoryView, id: UUID) = {
      val results: List[TaskInvocationRepresentation] = view.invocationResults.map(TaskInvocationRepresentation(_))
      TaskHistoryQueried(results)(id)
    }
  }

}

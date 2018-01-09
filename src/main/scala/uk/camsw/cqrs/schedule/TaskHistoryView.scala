package uk.camsw.cqrs.schedule

import uk.camsw.cqrs.View
import uk.camsw.cqrs.schedule.Events.TaskCompleted
import uk.camsw.cqrs.schedule.Models.InvocationResult
import uk.camsw.cqrs.schedule.Queries.QueryTaskHistory

case class TaskHistoryView(maxHistory: Int, invocationResults: List[InvocationResult] = List()) extends View[QueryTaskHistory, TaskHistoryView] {

  handleQuery { case query: QueryTaskHistory => id => query.execute(this, id) }

  onEvent { case TaskCompleted(r, _) =>
    this.copy(invocationResults = (r :: invocationResults) take maxHistory)
  }
}

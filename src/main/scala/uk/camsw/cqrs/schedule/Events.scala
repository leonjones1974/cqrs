package uk.camsw.cqrs.schedule

import java.util.UUID

import uk.camsw.cqrs.Event
import uk.camsw.cqrs.schedule.Models.InvocationResult

object Events {

  case class TaskCompleted(data: InvocationResult, override val id: UUID) extends Event[InvocationResult]
}

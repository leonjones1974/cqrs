package uk.camsw.cqrs.schedule

import org.joda.time.Instant
import uk.camsw.cqrs.schedule.Models.Task
import uk.camsw.cqrs.{DomainCommand, QueryCommand}

object Commands {

  trait ScheduleDomainCommand extends DomainCommand
  case class InvokeDue(now: Instant) extends ScheduleDomainCommand
  case class InvokeNow(task: Task[_]) extends ScheduleDomainCommand

}

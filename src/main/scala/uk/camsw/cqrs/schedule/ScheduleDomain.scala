package uk.camsw.cqrs.schedule

import java.util.UUID

import grizzled.slf4j.Logging
import org.joda.time.{Duration, Instant}
import uk.camsw.cqrs._
import uk.camsw.cqrs.schedule.Commands.{InvokeDue, InvokeNow, ScheduleDomainCommand}
import uk.camsw.cqrs.schedule.Events.TaskCompleted
import uk.camsw.cqrs.schedule.Models.{InvocationResult, Task, TaskId}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class ScheduleDomain(tasks: mutable.ListBuffer[Task[_]])(domainBus: EventBus)
  extends Domain[ScheduleDomainCommand, ScheduleDomain]
    with Logging {

  private val monitor = new AnyRef
  val due: mutable.Map[TaskId, Instant] = mutable.Map.empty

  handleCorrelatedCommand { case InvokeDue(now) =>
    correlationId =>
      var dueTasks: List[Task[_]] = List()
      monitor.synchronized {
        dueTasks = for {task <- tasks.toList if isDue(task, now)} yield task
        for {t <- dueTasks} due += (t.id -> t.nextDue(now))
      }
      for {
        t <- dueTasks
        events <- execute(now, t, correlationId)
      } yield events
  }

  handleCorrelatedCommand { case InvokeNow(task) => correlationId => execute(Instant.now(), task, correlationId) }

  val execute: (Instant, Task[_], UUID) => List[Event[_]] = {
    case (now, task, id) =>
      debug(s"Invoking task: [${task.id}]")
      val result = task.invoke()
      val elapsed = Duration.millis(Instant.now().getMillis - now.getMillis)
      debug(s"Task completed: [${task.id}] in [${elapsed}ms]")
      val invocationResult = InvocationResult(task, now, elapsed, result)
      val events = TaskCompleted(invocationResult, id) :: Nil
      for {e <- events} domainBus << e
      events
  }

  val nextDue: InvocationResult => Instant = r => r.startTime plus r.task.freq

  def isDue(task: Task[_], now: Instant) = neverRun(task) || runDue(task, now)

  def runDue(task: Task[_], now: Instant) = due(task.id).isBefore(now) || due(task.id) == now

  val neverRun: Task[_] => Boolean = t => !(due contains t.id)
}

object ScheduleDomain {

  def apply(tasks: Task[_]*)(domainBus: EventBus): ScheduleDomain = ScheduleDomain(ListBuffer() ++ tasks.toList)(domainBus)
}


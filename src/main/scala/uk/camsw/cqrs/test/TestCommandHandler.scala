package uk.camsw.cqrs.test

import uk.camsw.cqrs.EventBus.EventSeq
import uk.camsw.cqrs.{Command, CommandHandler, Event}

case class TestCommandHandler[A <: Command[_]](events: List[Event[_]] = List.empty) extends CommandHandler[A] {
  var received = List.empty[Any]
  var receiveThreads = List.empty[Long]

  override def handle: A => EventSeq = c => {
    received = received :+ c.data
    receiveThreads = receiveThreads :+ Thread.currentThread().getId
    events
  }
}

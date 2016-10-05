package uk.camsw.cqrs

import uk.camsw.cqrs.EventBus.EventList

case class TestCommandHandler[A <: Command[_]](events: List[Event[_]] = List.empty) extends CommandHandler[A] {
  var received = List.empty[Any]
  var receiveThreads = List.empty[Long]

  override def handle: A => EventList = c => {
    received = received :+ c.data
    receiveThreads = receiveThreads :+ Thread.currentThread().getId
    events
  }
}

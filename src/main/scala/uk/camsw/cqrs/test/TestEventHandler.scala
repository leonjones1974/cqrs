package uk.camsw.cqrs.test

import java.util.UUID

import uk.camsw.cqrs.{Event, EventHandler}

//todo: Check we really need these now the event handler is simplified
class StringEventHandler extends EventHandler {
  var received = List.empty[String]

  override def onEvent: (Event[_]) => Unit = e => e.data match {
    case s: String =>
      received = received :+ s
    case _ => ()
  }
}

class IntEventHandler extends EventHandler {
  var received = List.empty[Int]
  var receiveThreads = List.empty[Long]

  override def onEvent: (Event[_]) => Unit = e => e.data match {
    case n: Int =>
      received = received :+ n
      receiveThreads = receiveThreads :+ Thread.currentThread().getId
    case s => ()
  }
}

case class TestEvent[A](data: A, override val id: UUID = UUID.randomUUID()) extends Event[A]


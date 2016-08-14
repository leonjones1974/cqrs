package com.cam.cqrs.test

import com.cam.cqrs.{Command, CommandHandler, Event}

case class TestCommandHandler[A <: Command[_]](events: List[Event[_]] = List.empty) extends CommandHandler[A] {
  var received = List.empty[Any]

  override def handle: A => List[Event[_]] = c => {
    received = received :+ c.data
    events
  }
}

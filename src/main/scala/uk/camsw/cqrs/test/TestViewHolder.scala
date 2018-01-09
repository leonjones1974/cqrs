package uk.camsw.cqrs.test

import uk.camsw.cqrs.EventBus.EventSeq
import uk.camsw.cqrs._

import scala.reflect.ClassTag

case class TestViewHolder[A <: QueryCommand, B](a: View[A, B])(implicit bus: EventBus, tag: ClassTag[A]) extends ViewHolder[A, B](a){
  var raisedEvents = List.empty[Event[_]]

  override def handleCommand: Command[A] => EventSeq = c => {
    val events = super.handleCommand(c)
    raisedEvents = raisedEvents ++ events
    events
  }

  def <<(c: Command[_])(implicit tag: ClassTag[A]): TestViewHolder[A, B] = {
    bus << c
    this
  }

  def <<(ev: Event[_]): TestViewHolder[A, B] = {
    bus << ev
    this
  }

  def ?(c: Command[_])(implicit tag: ClassTag[A]) = bus ? c

}

object TestView {
  def apply[A <: QueryCommand, B](view: View[A, B])(implicit bus: EventBus, tag: ClassTag[A]): TestViewHolder[A, B] = {
    TestViewHolder(view)
  }
}
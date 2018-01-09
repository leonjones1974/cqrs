package uk.camsw.cqrs.test

import uk.camsw.cqrs.EventBus.EventSeq
import uk.camsw.cqrs._

import scala.reflect.ClassTag

case class TestDomainHolder[A <: DomainCommand, B](a: Domain[A, B])(implicit bus: EventBus, tag: ClassTag[A]) extends DomainHolder[A, B](a){
  var raisedEvents = List.empty[Event[_]]

  override def handleCommand: Command[A] => EventSeq = c => {
    val events = super.handleCommand(c)
    raisedEvents = raisedEvents ++ events
    events
  }

  def clearEvents() = raisedEvents = List()

  def <<(c: Command[_])(implicit tag: ClassTag[A]): TestDomainHolder[A, B] = {
    bus << c
    this
  }

  def <<(ev: Event[_]): TestDomainHolder[A, B] = {
    bus << ev
    this
  }

  def ?(c: Command[_])(implicit tag: ClassTag[A]) = bus ? c

  override def onPostUpdateEvents(xs: EventSeq) = raisedEvents = raisedEvents ++ xs
}

object TestDomain {
  def apply[A <: DomainCommand, B](domain: Domain[A, B])(implicit bus: EventBus, tag: ClassTag[A]): TestDomainHolder[A, B] = {
    TestDomainHolder(domain)
  }
}
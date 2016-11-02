package uk.camsw.cqrs

import scala.reflect.ClassTag

case class TestActorHolder[A <: Command[C], B, C](var actor: Actor[A, B, C])(implicit bus: EventBus, tag: ClassTag[A]) {
  var raisedEvents = List.empty[Event[_]]
  val eventBus = bus

  bus :+ new CommandHandler[A] {
    override def handle = c => {
      val events = actor.ch(c)
      raisedEvents = raisedEvents ++ events
      events
    }
  }

  bus :+ new EventHandler[Actor[A, B, C]] {
    override def onEvent = ev => {
      actor = actor.eh(ev)
      actor
    }
  }

  def <<(c: A)(implicit tag: ClassTag[A]): TestActorHolder[A, B, C] = {
    bus << c
    this
  }

  def <<(ev: Event[_]): TestActorHolder[A, B, C] = {
    bus << ev
    this
  }

}

object TestActor {
  def apply[A <: Command[C], B, C](actor: Actor[A, B, C])(implicit bus: EventBus, tag: ClassTag[A]): TestActorHolder[A, B, C] = {
    TestActorHolder(actor)
  }
}
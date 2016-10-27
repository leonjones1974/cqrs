package uk.camsw.cqrs

import scala.reflect.ClassTag

case class TestActorHolder[A <: Command[_], B](var actor: Actor[A, B])(implicit bus: EventBus, tag: ClassTag[A]) {
  var raisedEvents = List.empty[Event[_]]
  var raisedCommands = List.empty[Command[_]]
  val eventBus = bus

  bus :+ new CommandHandler[A] {
    override def handle = c => {
      val events = actor.ch(c)
      events
    }
  }

  bus :+ new EventHandler[Actor[A, B]] {
    override def onEvent = ev => {
      actor = actor.eh(ev)
      actor
    }
  }

  def <<(c: A)(implicit tag: ClassTag[A]): TestActorHolder[A, B] = {
    bus << c
    raisedCommands = raisedCommands :+ c
    this
  }

  def <<(ev: Event[_]): TestActorHolder[A, B] = {
    bus << ev
    raisedEvents = raisedEvents :+ ev
    this
  }

}

object TestActor {
  def apply[A <: Command[_], B](actor: Actor[A, B])(implicit bus: EventBus, tag: ClassTag[A]): TestActorHolder[A, B] = {
    TestActorHolder(actor)
  }
}
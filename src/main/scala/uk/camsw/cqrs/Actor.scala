package uk.camsw.cqrs

import uk.camsw.cqrs.EventBus.EventList

import scala.reflect.ClassTag

trait Actor[A, B] {

  def ch(cmd: A)(implicit bus: EventBus): EventList

  val eh: Event[_] => Actor[A, B]
}

case class ActorHolder[A <: Command[_], B](var actor: Actor[A, B])(implicit bus: EventBus, tag: ClassTag[A]) {
  val cmdSubscription = bus :+ new CommandHandler[A] {
    override def handle = actor.ch
  }

  val evSubscription = bus :+ new EventHandler[Actor[A, B]] {
    override def onEvent = ev => {
      actor = actor.eh(ev)
      actor
    }
  }

  def dispose() {
    cmdSubscription()
    evSubscription()
  }
}

object Actor {
  def apply[A <: Command[_], B](actor: Actor[A, B])(implicit bus: EventBus, tag: ClassTag[A]): ActorHolder[A, B] = {
    ActorHolder(actor)
  }
}


package uk.camsw.cqrs

import uk.camsw.cqrs.EventBus.EventList

import scala.reflect.ClassTag

trait Actor[A <: Command[C], B, C] {
  def ch(cmd: A)(implicit bus: EventBus): EventList

  val eh: Event[_] => Actor[A, B, C]

  type Data = C
  type Handler = PartialFunction[A, EventList]
  type ProducingHandler = EventBus => Handler

}

case class ActorHolder[A <: Command[C], B, C](var actor: Actor[A, B, C])(implicit bus: EventBus, tag: ClassTag[A]) {
  bus :+ new CommandHandler[A] {
    override def handle = actor.ch
  }

  bus :+ new EventHandler[Actor[A, B, C]] {
    override def onEvent = ev => {
      actor = actor.eh(ev)
      actor
    }
  }
}

object Actor {
  def apply[A <: Command[C], B, C](actor: Actor[A, B, C])(implicit bus: EventBus, tag: ClassTag[A]): ActorHolder[A, B, C] = {
    ActorHolder(actor)
  }
}


package uk.camsw.cqrs

import uk.camsw.cqrs.EventBus.{EmptyEventList, EventList}

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

trait Actor[A <: Command[_], B] {
  def ch(cmd: A)(implicit bus: EventBus): EventList

  val eh: Event[_] => Actor[A, B]
}


abstract class BaseActor[A: TypeTag, B] extends Actor[Command[A], B] {
  val CmdData = typeTag[A].tpe
  val % = CmdData
  type Handler = PartialFunction[Command[A], EventList]
  type ProducingHandler = EventBus => Handler

  val doNothing: Handler = {
    case _ => EmptyEventList
  }
}

case class ActorHolder[A <: Command[_], B](var actor: Actor[A, B])(implicit bus: EventBus, tag: ClassTag[A]) {
  bus :+ new CommandHandler[A] {
    override def handle = actor.ch
  }

  bus :+ new EventHandler[Actor[A, B]] {
    override def onEvent = ev => {
      actor = actor.eh(ev)
      actor
    }
  }
}

object Actor {
  def apply[A <: Command[_], B](actor: Actor[A, B])(implicit bus: EventBus, tag: ClassTag[A]): ActorHolder[A, B] = {
    ActorHolder(actor)
  }
}


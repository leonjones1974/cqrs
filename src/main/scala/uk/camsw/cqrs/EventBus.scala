package uk.camsw.cqrs

import java.util.UUID

import scala.concurrent.{Future, Promise}
import scala.reflect.ClassTag
import scalaz.Scalaz._

abstract class Command[A](val _id: UUID = UUID.randomUUID()) {
  def id: UUID = _id
  def data: A
}

object Command {
  implicit def asCommand[A]: A => Command[A] = c => new Command[A] {
    val data = c
  }
}

abstract class Event[A](val _id: UUID = UUID.randomUUID()) {
  def id: UUID = _id
  def data: A
}

object Event {
  implicit def asEvent[A]: A => Event[A] = e => new Event[A] {
    val data = e
    override val id: UUID = UUID.randomUUID()
  }
}

trait CommandHandler[A] {
  def handle: A => List[Event[_]]
}

trait EventHandler[A] {
  def onEvent: Event[_] => A
}

case class EventBus(var commandHandlers: Map[ClassTag[_], List[CommandHandler[_]]] = Map.empty.withDefaultValue(List.empty),
                    var eventHandlers: List[EventHandler[_]] = List.empty) {

  type Subscription = () => Unit

  def :+[A](h: CommandHandler[A])(implicit tag: ClassTag[A]): Subscription = {
    commandHandlers = commandHandlers + (tag -> (~commandHandlers.get(tag) :+ h))
    () => Unit
  }

  def :+[A](e: EventHandler[A]): Subscription = {
    eventHandlers = e :: eventHandlers
    () => {
      eventHandlers = eventHandlers.filterNot(_ == e)
    }
  }

  def ?[A <: Command[_]](c: A)(implicit tag: ClassTag[A]): Future[Event[_]] = {
    val p = Promise[Event[_]]
    var unsubscribe: Subscription = null
    unsubscribe = this :+ new EventHandler[EventBus] {
      override def onEvent: (Event[_]) => EventBus = {
        case e if e.id == c.id =>
          unsubscribe()
          p.success(e)
          EventBus.this
        case _ => EventBus.this
      }
    }
    this << c
    p.future
  }

  def <<[A <: Command[_]](c: A)(implicit tag: ClassTag[A]): List[Event[_]] = {
    val h = ~commandHandlers.get(tag)
    val events = h.flatMap(x => x.asInstanceOf[CommandHandler[A]].handle(c))
    events.foreach(this << _)
    events
  }

  def <<(ev: Event[_]): EventBus = {
    eventHandlers.foreach(_.onEvent(ev))
    this
  }
}

object EventBus {
  type Subscription = () => Unit
}

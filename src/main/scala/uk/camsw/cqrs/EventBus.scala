package uk.camsw.cqrs

import scala.reflect.ClassTag
import scalaz.Scalaz._

trait Command[A] {
  val data: A
}

object Command {
  implicit def asCommand[A]: A => Command[A] = c => new Command[A] {
    val data = c
  }
}

trait Event[A] {
  val data: A
}

object Event {
  implicit def asEvent[A]: A => Event[A] = e => new Event[A] {
    val data = e
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

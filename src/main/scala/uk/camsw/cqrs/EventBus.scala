package uk.camsw.cqrs

import java.util.UUID

import grizzled.slf4j.Logging
import uk.camsw.cqrs.EventBus.{EventList, Subscription}

import scala.concurrent.{ExecutionContext, Future, Promise}
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

case class EventBus(_executionContext: ExecutionContext, var commandHandlers: Map[ClassTag[_], List[CommandHandler[_]]] = Map.empty.withDefaultValue(List.empty),
                    var eventHandlers: List[EventHandler[_]] = List.empty) extends Logging {

  implicit val executionContext = _executionContext
  def :+[A](h: CommandHandler[A])(implicit tag: ClassTag[A]): Subscription = {
    commandHandlers = commandHandlers + (tag -> (~commandHandlers.get(tag) :+ h))
    () => commandHandlers = commandHandlers + (tag -> (~commandHandlers.get(tag)).filterNot(_ == h))
  }

  def :+[A](e: EventHandler[A]): Subscription = {
    eventHandlers = e :: eventHandlers
    () => {
      eventHandlers = eventHandlers.filterNot(_ == e)
    }
  }

  def ?[A <: Command[_]](c: A)(implicit tag: ClassTag[A]) =
    (this << c).map(xs => xs.find(_.id == c.id))

  def <<[A <: Command[_]](c: A)(implicit tag: ClassTag[A]): Future[EventList] = {
    debug(s"Publishing Command: [$c]")
    val p = Promise[EventList]
    Future {
      debug(s"Executing Command: [$c]")
      val handlers = ~commandHandlers.get(tag)
      val events = handlers.flatMap(x => x.asInstanceOf[CommandHandler[A]].handle(c))
      events.foreach(this << _)
      p.success(events)
    }
    p.future
  }

  def <<(ev: Event[_]): EventBus = {
    debug(s"Publish Event: [$ev]")
    eventHandlers.foreach(eh => {
      try {
        eh.onEvent(ev)
      } catch {
        case e:Exception =>
          error(s"Got exception: [$e]")
          e.printStackTrace()
      }
    })
    this
  }
}

object EventBus {
  type Subscription = () => Unit
  type EventList = List[Event[_]]
}

package uk.camsw.cqrs

import java.util.UUID

import grizzled.slf4j.Logging
import uk.camsw.cqrs.EventBus.{EventSeq, Subscription}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}
import scalaz.Scalaz._


// todo: make this a final case class
abstract class Command[A](val _id: UUID = UUID.randomUUID()) {
  def id: UUID = _id

  def data: A

  override def toString: String = s"Command($data)"
}


object Command {
  implicit def asCommand[A]: A => Command[A] = c => new Command[A] {
    val data = c
  }
}

abstract class Event[A](val _id: UUID = UUID.randomUUID()) {
  val id: UUID = _id

  def data: A
}


trait CommandHandler[A] {
  def handle: A => EventSeq
}

trait EventHandler {
  def onEvent: Event[_] => Unit
}

case class EventBus(_executionContext: ExecutionContext, var commandHandlers: Map[ClassTag[_], List[CommandHandler[_]]] = Map.empty.withDefaultValue(List.empty),
                    var eventHandlers: List[EventHandler] = List.empty) extends Logging {

  implicit val executionContext = _executionContext

  def :+[A](h: CommandHandler[A])(implicit tag: ClassTag[A]): Subscription = {
    commandHandlers = commandHandlers + (tag -> (~commandHandlers.get(tag) :+ h))
    () => commandHandlers = commandHandlers + (tag -> (~commandHandlers.get(tag)).filterNot(_ == h))
  }

  def :+[A](e: EventHandler): Subscription = {
    eventHandlers = e :: eventHandlers
    () => {
      eventHandlers = eventHandlers.filterNot(_ == e)
    }
  }

  def ?[A <: Command[_]](c: A)(implicit tag: ClassTag[A]): Future[Option[Event[_]]] = (this << c).map(xs => xs.find(_.id == c.id))

  def ?[A <: QueryCommand](c: A)(implicit tag: ClassTag[Command[A]]): Future[Option[Event[_]]] = this ? Command.asCommand(c)

  def <<[A <: DomainCommand](c: A)(implicit tag: ClassTag[Command[A]]): Future[EventSeq] = this << Command.asCommand(c)

  def <<[A <: Command[_]](c: A)(implicit tag: ClassTag[A]): Future[EventSeq] = {
    debug(s"Publishing Command: [$c]")
    val p = Promise[EventSeq]
    Future {
      Try {
        debug(s"Executing Command: [$c]")
        val handlers = ~commandHandlers.get(tag)
        debug(s"Found handlers: [${handlers.size}]")
        val events = handlers.flatMap(x => x.asInstanceOf[CommandHandler[A]].handle(c))
        events.foreach(this << _)
        p.success(events)
      } match {
        case Success(p) => p
        case Failure(t) =>
          error("Failure during command publishing", t)
          p.failure(t)
      }
    }
    p.future
  }

  def <<(ev: Event[_]): EventBus = {
    debug(s"Publish Event: [$ev]")
    Try{eventHandlers.foreach(eh => eh.onEvent(ev))} match {
      case Success(_) => this
      case Failure(t) =>
        error("Failure during event publishing", t)
        this
    }
  }
}

object EventBus {
  type Subscription = () => Unit
  type EventSeq = Seq[Event[_]]
}

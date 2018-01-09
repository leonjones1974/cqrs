package uk.camsw.cqrs

import java.util.UUID

import grizzled.slf4j.Logging
import uk.camsw.cqrs.EventBus.EventSeq

import scala.collection.mutable
import scala.reflect.ClassTag

// todo: Dedup all the view/ query stuff when happy with it
trait DomainCommand

trait Domain[A <: DomainCommand, B] {
  type DomainType = Domain[A, B]
  type EventHandler = =>?[Event[_], DomainType]
  type PostEventHandler = =>?[Event[_], EventSeq]

  type CorrelatedCommandHandler = =>?[Any, UUID => EventSeq]

  private[cqrs] val commandHandlers = mutable.ListBuffer[Domain.CommandHandler]()
  private[cqrs] val correlatedCommandHandlers = mutable.ListBuffer[CorrelatedCommandHandler]()
  private[cqrs] val eventHandlers = mutable.ListBuffer[EventHandler]()
  private[cqrs] val postEventHandlers = mutable.ListBuffer[PostEventHandler]()

  def onEvent(f: EventHandler): Unit = eventHandlers += f

  def postEvent(f: PostEventHandler): Unit = postEventHandlers += f

  def handleCommand(f: Domain.CommandHandler) = commandHandlers += f

  def handleCorrelatedCommand(f: CorrelatedCommandHandler) = correlatedCommandHandlers += f

  implicit def asEventSeq(ev: Event[_]): EventSeq = Seq(ev)
}

object Domain {

  def apply[A <: DomainCommand, B](domain: Domain[A, B])(implicit bus: EventBus, tag: ClassTag[A]): DomainHolder[A, B] = {
    new DomainHolder(domain)
  }

  type CommandHandler = =>?[DomainCommand, EventSeq]
}


class DomainHolder[A <: DomainCommand, B](var domain: Domain[A, B])(implicit bus: EventBus, tag: ClassTag[A]) extends Logging {

  private val nullEventHandler: Event[_] =>? Domain[A, B] = {
    case _ => domain
  }
  private val nullPostEventHandler: =>? [Event[_], EventSeq] = {
    case _ => List()
  }

  private val nullCommandHandler: Any =>? EventSeq = {
    case _ => Nil
  }

  private def pfEvent = (domain.eventHandlers.toList :+ nullEventHandler).reduce(_ orElse _)

  private def pfPostEvent = (domain.postEventHandlers.toList :+ nullPostEventHandler).reduce(_ orElse _)

  private def pfCommand = domain.commandHandlers.toList.reduce(_ orElse _)

  private def pfCorrelated = domain.correlatedCommandHandlers.toList.reduce(_ orElse _)

  //todo: need to split query and command up - or basically rewrite the cqrs stuff :(
  def handleCommand: Command[A] => EventSeq = cmd => {
    try {
      if (cmd.data.isInstanceOf[DomainCommand] && domain.commandHandlers.nonEmpty && pfCommand.isDefinedAt(cmd.data)) {
        debug(s"[${cmd.data}] handled by: ${domain.getClass.getSimpleName}")
        pfCommand(cmd.data)
      }
      else if (domain.correlatedCommandHandlers.nonEmpty && pfCorrelated.isDefinedAt(cmd.data)) {
        pfCorrelated(cmd.data)(cmd.id)
      }
      else {
        nullCommandHandler(cmd.data)
      }
    } catch {
      case e: Exception =>
        Nil
    } finally {
    }
  }

  val cmdSubscription = bus :+ new CommandHandler[Command[A]] {
    override def handle = handleCommand
  }

  val evSubscription = bus :+ new EventHandler {
    override def onEvent = ev => {
      domain = pfEvent(ev)
      val postUpdateEvents = pfPostEvent(ev)
      postUpdateEvents foreach {bus << _}
      onPostUpdateEvents(postUpdateEvents)
    }
  }

  protected def onPostUpdateEvents(xs: EventSeq): Unit = {
  }

  def dispose() {
    cmdSubscription()
    evSubscription()
  }
}



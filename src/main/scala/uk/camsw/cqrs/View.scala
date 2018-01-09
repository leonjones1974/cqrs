package uk.camsw.cqrs

import java.util.UUID

import grizzled.slf4j.Logging
import uk.camsw.cqrs.EventBus.EventSeq
import uk.camsw.cqrs.View.QueryHandler

import scala.collection.mutable
import scala.reflect.ClassTag

trait QueryCommand

trait ViewQueried[A] extends Event[List[A]] {
  self =>
  val data: List[A]
  override val id: UUID
}

trait View[A <: QueryCommand, B] {
  type ViewType = View[A, B]
  type EventHandler = =>?[Event[_], View[A, B]]

  def handleQuery(f: QueryHandler) = queryHandlers += f

  private[cqrs] val eventHandlers = mutable.ListBuffer[EventHandler]()

  def onEvent(f: EventHandler): Unit = eventHandlers += f

  private[cqrs] val queryHandlers = mutable.ListBuffer[QueryHandler]()

  implicit def asEventSeq(ev: Event[_]): EventSeq = Seq(ev)
}

object View {
  type QueryHandler = =>?[QueryCommand, UUID => EventSeq]

  def apply[A <: QueryCommand, B](view: View[A, B])(implicit bus: EventBus, tag: ClassTag[A]): ViewHolder[A, B] = {
    new ViewHolder(view)
  }

}

class ViewHolder[A <: QueryCommand, B](var view: View[A, B])(implicit bus: EventBus, tag: ClassTag[A])
  extends Logging {
  private val nullEventHandler: Event[_] =>? View[A, B] = {
    case _ => view
  }

  private def pfQuery = view.queryHandlers.toList.reduce(_ orElse _)

  private def pfEvent = (view.eventHandlers.toList :+ nullEventHandler).reduce(_ orElse _)

  private val nullCommandHandler: Any =>? EventSeq = {
    case _ => Nil
  }

  val cmdSubscription = bus :+ new CommandHandler[Command[A]] {
    override def handle = handleCommand
  }

  // todo: refactor this so the id is auto injected
  def handleCommand: Command[A] => EventSeq = cmd => {
    try {
      if (cmd.data.isInstanceOf[QueryCommand] && view.queryHandlers.nonEmpty && pfQuery.isDefinedAt(cmd.data)) {
        debug(s"[${cmd.data}] handled by: ${view.getClass.getSimpleName}")
        pfQuery(cmd.data)(cmd.id)
      }
      else nullCommandHandler(cmd.data)
    } catch {
      case e: Exception =>
        error(s"Failure during handling of command: [$cmd]", e)
        Nil
    }
  }

  val evSubscription = bus :+ new EventHandler {
    override def onEvent = ev => {
      view = pfEvent(ev)
    }
  }

  def dispose() {
    cmdSubscription()
    evSubscription()
  }
}

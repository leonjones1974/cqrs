package uk.camsw.cqrs

import uk.camsw.cqrs.EventBus.EventList

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

trait ServerAssembly {
  implicit val executionContext: ExecutionContext

  implicit val bus = EventBus()

  def withActor[A <: Command[_], B](actor:  Actor[A, B])(implicit tag: ClassTag[A]) = Actor(actor)(bus, tag)

  def <<[A <: Command[_]](cmd: A)(implicit tag: ClassTag[A]): Future[EventList] = bus << cmd
}

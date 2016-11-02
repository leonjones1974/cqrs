package uk.camsw.cqrs

import uk.camsw.cqrs.EventBus.EventList

import scala.concurrent.Future
import scala.reflect.ClassTag

trait ServerAssembly {
  this: ServerAssembly =>
  implicit val bus: EventBus

  def withActor[A <: Command[C], B, C](actor:  Actor[A, B, C])(implicit tag: ClassTag[A]) = Actor(actor)(bus, tag)

  def <<[A <: Command[_]](cmd: A)(implicit tag: ClassTag[A]): Future[EventList] = bus << cmd
}

package uk.camsw.cqrs

import scala.reflect.ClassTag

trait ServerAssembly {

  implicit val bus = EventBus()

  def withActor[A <: Command[_], B](actor:  Actor[A, B])(implicit tag: ClassTag[A]) = Actor(actor)(bus, tag)

  def <<[A <: Command[_]](cmd: A)(implicit tag: ClassTag[A]): List[Event[_]] = bus << cmd
}

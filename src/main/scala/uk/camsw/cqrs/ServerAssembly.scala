package uk.camsw.cqrs

import scala.reflect.ClassTag

trait ServerAssembly {

  implicit val bus = EventBus()

  def <<[A <: Command[_]](cmd: A)(implicit tag: ClassTag[A]): List[Event[_]] = bus << cmd
}

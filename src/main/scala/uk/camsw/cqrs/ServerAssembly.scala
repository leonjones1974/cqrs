package uk.camsw.cqrs

import scala.reflect.ClassTag

trait ServerAssembly {
  this: ServerAssembly =>
  implicit val bus: EventBus

  def withDomain[A <: DomainCommand, B](domain:  Domain[A, B])(implicit tag: ClassTag[A]) = Domain(domain)(bus, tag)
  def withView[A <: QueryCommand, B](view:  View[A, B])(implicit tag: ClassTag[A]) = View(view)(bus, tag)

}

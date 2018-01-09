package uk.camsw.cqrs.hospice

import uk.camsw.cqrs.DomainCommand
import uk.camsw.cqrs.hospice.Models.InvalidData

object Commands {

  trait HospiceDomainCommand extends DomainCommand
  case class RegisterInvalidData(data: InvalidData) extends HospiceDomainCommand

}

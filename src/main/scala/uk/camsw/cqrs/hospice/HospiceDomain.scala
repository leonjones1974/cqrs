package uk.camsw.cqrs.hospice

import grizzled.slf4j.Logging
import uk.camsw.cqrs.Domain
import uk.camsw.cqrs.hospice.Commands.{HospiceDomainCommand, RegisterInvalidData}
import uk.camsw.cqrs.hospice.Events.DataHospiced

case class HospiceDomain()
  extends Domain[HospiceDomainCommand, HospiceDomain] with Logging {

  handleCommand { case RegisterInvalidData(invalidData) => DataHospiced(invalidData) }

}

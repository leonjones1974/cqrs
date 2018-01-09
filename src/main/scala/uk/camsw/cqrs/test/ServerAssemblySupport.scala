package uk.camsw.cqrs.test

import org.scalatest.{BeforeAndAfter, Suite}
import uk.camsw.cqrs.{Domain, DomainCommand, ServerAssembly}

import scala.reflect._

trait ServerAssemblySupport extends BeforeAndAfter {
  self: Suite =>

  var server: ServerAssembly = _
  var collector: TestDomainHolder[NullDomainCommand, NullDomain] = _

  before {
    server = serverAssembly()
    collector = TestDomainHolder(NullDomain())(server.bus, classTag[NullDomainCommand])
  }

  def raisedEvents = collector.raisedEvents

  def serverAssembly(): ServerAssembly
}

case class NullDomainCommand(data: String = "") extends DomainCommand

case class NullDomain() extends Domain[NullDomainCommand, NullDomain] {
}



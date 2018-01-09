package uk.camsw.cqrs.test

import com.google.common.util.concurrent.MoreExecutors
import org.scalatest.{BeforeAndAfter, Suite}
import uk.camsw.cqrs._
import uk.camsw.cqrs.hospice.Commands.HospiceDomainCommand
import uk.camsw.cqrs.hospice.Models.InvalidData
import uk.camsw.cqrs.hospice.Queries.{HospiceQueried, QueryHospice}
import uk.camsw.cqrs.hospice.{HospiceDomain, HospiceView, Queries}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps
import scala.reflect.ClassTag

trait DomainTestSupport[A <: DomainCommand, B] extends BeforeAndAfter {
  self: Suite =>

  def commandTag: ClassTag[A]

  implicit val executionContext = ExecutionContext.fromExecutor(MoreExecutors.directExecutor())
  implicit var bus: EventBus = _

  def domainUnderTest(): Domain[A, B]

  var domain: TestDomainHolder[A, B] = _
  var hospice: TestDomainHolder[HospiceDomainCommand, HospiceDomain] = _
  var hospiceView: TestViewHolder[QueryHospice, HospiceDomain] = _

  def given(block: => Unit) = {
    block
    domain.clearEvents()
  }

  def hospicedData(): List[InvalidData] = {
    val f = hospiceView ? Queries.All map {
      case Some(HospiceQueried(d)) => d
      case _ => throw new RuntimeException("Query failed to return")
    }
    Await.result(f, 4 seconds)
  }

  before {
    bus = EventBus(executionContext)
    val test: Domain[A, B] = domainUnderTest()

    hospice = TestDomain(HospiceDomain())
    hospiceView = TestView(HospiceView())
    domain = TestDomain(test)(bus, commandTag)
  }
}

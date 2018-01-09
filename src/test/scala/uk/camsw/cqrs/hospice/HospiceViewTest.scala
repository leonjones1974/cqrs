package uk.camsw.cqrs.hospice

import com.google.common.util.concurrent.MoreExecutors
import org.scalatest.{FunSpec, Matchers}
import uk.camsw.cqrs.hospice.Commands._
import uk.camsw.cqrs.hospice.Events.DataHospiced
import uk.camsw.cqrs.hospice.Models.HospiceEntryType.Test
import uk.camsw.cqrs.hospice.Models.InvalidData
import uk.camsw.cqrs.hospice.Queries.{HospiceQueried, QueryHospice}
import uk.camsw.cqrs.test.ViewTestSupport

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class HospiceViewTest extends FunSpec with Matchers with ViewTestSupport[QueryHospice, HospiceDomain]{
  import HospiceViewTest._

  describe("Hospice domain") {
    it("should return no data initially") {
      view << Queries.All
      view.raisedEvents should contain only HospiceQueried(List())(anyId)
    }

    it("should return invalid data") {
      view << dataHospiced
      view << Queries.All
      view.raisedEvents should contain only HospiceQueried(List(invalidData))(anyId)
    }

    it("should be possible to ask and match a query") {
      view << dataHospiced
      implicit val ec = ExecutionContext.fromExecutor(MoreExecutors.directExecutor())
      val f = view ? Queries.All map {
        case Some(HospiceQueried(d)) => d
        case _ => throw new RuntimeException("Query failed to return")
      }
      Await.result(f, 4 seconds) shouldBe List(invalidData)
    }

  }
  override val queryTag = reflect.classTag[QueryHospice]

  override def viewUnderTest() = HospiceView()
}

object HospiceViewTest {
  val invalidData = InvalidData(Test, "some_invalid_data", "some_reason")
  val hospiceData = RegisterInvalidData(invalidData)
  val dataHospiced = DataHospiced(invalidData)
}
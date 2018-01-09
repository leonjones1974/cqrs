package uk.camsw.cqrs.test

import java.util.UUID

import com.google.common.util.concurrent.MoreExecutors
import org.scalatest.{BeforeAndAfter, Suite}
import uk.camsw.cqrs._

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

trait ViewTestSupport[A <: QueryCommand, B] extends BeforeAndAfter {
  self: Suite =>

  def queryTag: ClassTag[A]

  val executionContext = ExecutionContext.fromExecutor(MoreExecutors.directExecutor())
  implicit var bus: EventBus = _

  def viewUnderTest(): View[A, B]

  var view: TestViewHolder[A, B] = _

  def anyId = UUID.randomUUID()

  before {
    bus = EventBus(executionContext)
    val test: View[A, B] = viewUnderTest()
    view = TestView(test)(bus, queryTag)
  }
}

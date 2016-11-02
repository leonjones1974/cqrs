package uk.camsw.cqrs

import com.google.common.util.concurrent.MoreExecutors
import org.scalatest.{BeforeAndAfter, Suite}

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

trait ActorTestSupport[A <: Command[C], B, C] extends BeforeAndAfter {
  self: Suite =>

  val commandTag: ClassTag[A]

  val executionContext = ExecutionContext.fromExecutor(MoreExecutors.directExecutor())
  implicit var bus: EventBus = _

  def actorUnderTest(): Actor[A, B, C]

  var actorSystem: TestActorHolder[A, B, C] = _

  before {
    bus = EventBus(executionContext)
    actorSystem = TestActor(actorUnderTest())(bus, commandTag)
  }
}

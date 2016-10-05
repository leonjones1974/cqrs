package uk.camsw.cqrs

import com.google.common.util.concurrent.MoreExecutors
import org.scalatest.{BeforeAndAfter, Suite}

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

trait ActorTestSupport[A <: Command[_], B] extends BeforeAndAfter {
  self: Suite =>

  val commandTag: ClassTag[A]

  implicit val executionContext = ExecutionContext.fromExecutor(MoreExecutors.directExecutor())
  implicit var bus: EventBus = _

  def actorUnderTest(): Actor[A, B]

  var actorSystem: TestActorHolder[A, B] = _

  before {
    bus = EventBus()
    actorSystem = TestActor(actorUnderTest())(bus, commandTag)
  }
}

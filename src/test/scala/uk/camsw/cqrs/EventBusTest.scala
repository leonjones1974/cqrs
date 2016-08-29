package uk.camsw.cqrs

import java.util.UUID

import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class EventBusTest extends FunSpec with Matchers {

  import EventBusTest._
  import scala.concurrent.ExecutionContext.Implicits.global

  describe("Event Bus") {

    sut :+ ch1
    sut :+ ch2
    sut :+ eh1
    sut :+ eh2
    sut << stringCommand
    sut << intCommand

    it("should dispatch a command to registered handler") {
      ch1.received should contain only stringCommand.data
    }

    it("should dispatch commands to all registered handlers") {
      ch2.received should contain only intCommand.data
    }

    it("should dispatch events returned from command to registered handlers") {
      eh1.received shouldBe List(ev1a, ev1b, ev2a)
      eh2.received shouldBe List(intEvent)
    }
  }

  describe("Event Bus -> Request/ Response") {
    it ("ask should correlated first event based on command id") {
      val f = sut ? intCommand
      sut << TestEvent(ev1a, UUID.randomUUID())
      sut << TestEvent(ev1b, intCommand.id)

      f.onSuccess { case e => e.data shouldBe ev1b}
      Await.result(f, 5 seconds)
      f.onFailure{case t => fail(s"Future failed: $t")}
    }
  }

  describe("Event Bus -> Request/ Response") {
    it ("ask should drop subsequent matching commands") {
      val f = sut ? intCommand
      sut << TestEvent(ev1a, intCommand.id)
      sut << TestEvent(ev1b, intCommand.id)

      f.onSuccess { case e => e.data shouldBe ev1a}
      Await.result(f, 5 seconds)
      f.onFailure{case t => fail(s"Future failed: $t")}
    }
  }
}

object EventBusTest extends DataTestSupport {
  val sut = EventBus()

  val stringCommand = StringCommand(aString())

  val ev1a, ev1b, ev2a = aString()
  val intCommand = IntCommand(anInt())
  val intEvent = anInt()

  val ch1 = TestCommandHandler[StringCommand](List(ev1a, ev1b))
  val ch2 = TestCommandHandler[IntCommand](List(intEvent, ev2a))

  val eh1 = new StringEventHandler
  val eh2 = new IntEventHandler

}

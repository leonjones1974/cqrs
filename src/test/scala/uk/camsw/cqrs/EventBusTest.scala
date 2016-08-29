package uk.camsw.cqrs

import java.util.UUID
import java.util.concurrent.Executors

import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

class EventBusTest extends FunSpec with BeforeAndAfter with Matchers  {

  import EventBusTest._

  describe("Event Bus - Pub/ Sub") {
    val eventBus = EventBus()
    eventBus :+ ch1
    eventBus :+ ch2
    eventBus :+ eh1
    eventBus :+ eh2
    eventBus << stringCommand
    eventBus << intCommand

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
    it("ask should correlated first event based on command id") {
      val eventBus = EventBus()
      eventBus :+ TestCommandHandler[IntCommand](List(
        TestEvent(ev1a, UUID.randomUUID()),
        TestEvent(ev1b, intCommand.id))
      )
      Await.result(eventBus ? intCommand, 1 second).data shouldBe ev1b
    }

    it("ask should drop subsequent matching commands") {
      val eventBus = EventBus()
      eventBus :+ TestCommandHandler[IntCommand](List(
        TestEvent(ev1a, intCommand.id),
        TestEvent(ev1b, intCommand.id))
      )
      Await.result(eventBus ? intCommand, 1 second).data shouldBe ev1a
    }
  }
}

object EventBusTest extends DataTestSupport {
  implicit val context = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  val stringCommand = StringCommand(aString())

  val ev1a, ev1b, ev2a = aString()
  val intCommand = IntCommand(anInt())
  val intEvent = anInt()

  val ch1 = TestCommandHandler[StringCommand](List(ev1a, ev1b))
  val ch2 = TestCommandHandler[IntCommand](List(intEvent, ev2a))

  val eh1 = new StringEventHandler
  val eh2 = new IntEventHandler

}

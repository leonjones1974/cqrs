package uk.camsw.cqrs

import java.util.UUID
import java.util.concurrent.Executors
import scala.concurrent.duration._
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import scala.concurrent.{Await, ExecutionContext}

class EventBusTest extends FunSpec with BeforeAndAfter with Matchers with ConcurrencyTestSupport {

  implicit val executionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
  import EventBusTest._

  var eventBus: EventBus = _
  var stringCommandHandler: TestCommandHandler[StringCommand] = _
  var intCommandHandler: TestCommandHandler[IntCommand] = _
  var stringEventHandler: StringEventHandler = _
  var intEventHandler: IntEventHandler = _

  before {
    eventBus = EventBus()
    stringCommandHandler = TestCommandHandler[StringCommand](List(ev1a, ev1b))
    intCommandHandler = TestCommandHandler[IntCommand](List(intEvent, ev2a))
    stringEventHandler = new StringEventHandler
    intEventHandler = new IntEventHandler
  }

  describe("Event Bus - Pub/ Sub") {

    def initialise() {
      eventBus :+ stringCommandHandler
      eventBus :+ intCommandHandler
      eventBus :+ stringEventHandler
      eventBus :+ intEventHandler
      asyncAndWait({
        eventBus << stringCommand
        eventBus << intCommand
      }, times = 1)
    }

    it("should dispatch a command to registered handler") {
      initialise()
      stringCommandHandler.received should contain only stringCommand.data
    }

    it("should dispatch commands to all registered handlers") {
      initialise()
      intCommandHandler.received should contain only intCommand.data
    }

    it("should dispatch events returned from command to registered handlers") {
      initialise()
      stringEventHandler.received shouldBe List(ev1a, ev1b, ev2a)
      intEventHandler.received shouldBe List(intEvent)
    }
  }

  describe("Event Bus -> Request/ Response") {
    it("should return first correlated event based on command id, upon ask") {
      val eventBus = EventBus()
      eventBus :+ TestCommandHandler[IntCommand](List(
        TestEvent(ev1a, UUID.randomUUID()),
        TestEvent(ev1b, intCommand.id))
      )
      Await.result(eventBus ? intCommand, 1 second).data shouldBe ev1b
    }

    it("should drop subsequent matching commands, upon ask") {
      val eventBus = EventBus()
      eventBus :+ TestCommandHandler[IntCommand](List(
        TestEvent(ev1a, intCommand.id),
        TestEvent(ev1b, intCommand.id))
      )
      Await.result(eventBus ? intCommand, 1 second).data shouldBe ev1a
    }
  }

  describe("Event Bus - Concurrency") {

    it("should always execute commands on the same thread, regardless of publisher") {
      val eventBus = EventBus()
      val handler = TestCommandHandler[IntCommand](List.empty)
      eventBus :+ handler
      asyncAndWait(eventBus << intCommand, 3)

      handler.received.size shouldBe 3
      handler.receiveThreads.toSet.size shouldBe 1
    }

    it("should execute ask commands on the same publish event loop") {
      val eventBus = EventBus()
      val handler = TestCommandHandler[IntCommand](List.empty)
      eventBus :+ handler
      asyncAndWait(eventBus << intCommand, 1)
      asyncAndWait(eventBus ? intCommand, 3)

      handler.received.size shouldBe 4
      handler.receiveThreads.toSet.size shouldBe 1
    }

    it("should raise events on the publish thread") {
      val eventBus = EventBus()
      val handler = TestCommandHandler[IntCommand](List(intEvent))
      eventBus :+ handler
      eventBus :+ intEventHandler
      asyncAndWait(eventBus << intCommand, 3)
      intEventHandler.received.size shouldBe 3
      (handler.receiveThreads ++ intEventHandler.receiveThreads).toSet.size shouldBe 1
    }

    it("should raise events from an ask on the publish thread") {
      val eventBus = EventBus()
      val handler = TestCommandHandler[IntCommand](List(intEvent))
      eventBus :+ handler
      eventBus :+ intEventHandler
      asyncAndWait(eventBus ? intCommand, 3)
      intEventHandler.received.size should be > 1
      (handler.receiveThreads ++ intEventHandler.receiveThreads).toSet.size shouldBe 1
    }
  }
}

object EventBusTest extends DataTestSupport {
  val stringCommand = StringCommand(aString())
  val ev1a, ev1b, ev2a = aString()
  val intCommand = IntCommand(anInt())
  val intEvent = anInt()
}

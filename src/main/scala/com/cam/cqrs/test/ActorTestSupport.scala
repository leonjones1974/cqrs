package com.cam.cqrs.test

import com.cam.cqrs.{Actor, Command, EventBus}

import scala.reflect.ClassTag

trait ActorTestSupport[A <: Command[_], B] extends BeforeAndAfter {
  self: Suite =>

  val commandTag: ClassTag[A]

  implicit var bus: EventBus = _
  def actorUnderTest(): Actor[A, B]
  var actorSystem: TestActorHolder[A, B] = _

  before {
    bus = EventBus()
    actorSystem = TestActor(actorUnderTest())(bus, commandTag)
  }
}

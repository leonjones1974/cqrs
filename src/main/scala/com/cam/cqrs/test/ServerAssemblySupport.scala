package com.cam.cqrs.test

import com.cam.cqrs._

import scala.reflect._

/**
  * Created by leonjones on 14/08/16.
  */
trait ServerAssemblySupport extends BeforeAndAfter {
  self: Suite =>

  var server: ServerAssembly = _
  var collector: TestActorHolder[NullCommand, NullActor] = _

  before {
    server = ServerAssembly()
    collector = TestActorHolder(NullActor())(server.bus, classTag[NullCommand])
  }

  def raisedEvents = collector.raisedEvents
}

case class NullCommand() extends Command[String] {
  override val data: String = ""
}

case class NullActor() extends Actor[NullCommand, NullActor] {
  override def ch(cmd: NullCommand)(implicit bus: EventBus): List[Event[_]] = List()
  override val eh: (Event[_]) => Actor[NullCommand, NullActor] = ev => this
}



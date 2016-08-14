package uk.camsw.cqrs

import org.scalatest.{BeforeAndAfter, Suite}

import scala.reflect._

trait ServerAssemblySupport extends BeforeAndAfter {
  self: Suite =>

  var server: ServerAssembly = _
  var collector: TestActorHolder[NullCommand, NullActor] = _

  before {
    server = serverAssembly()
    collector = TestActorHolder(NullActor())(server.bus, classTag[NullCommand])
  }

  def raisedEvents = collector.raisedEvents

  def serverAssembly(): ServerAssembly
}

case class NullCommand() extends Command[String] {
  override val data: String = ""
}

case class NullActor() extends Actor[NullCommand, NullActor] {
  override def ch(cmd: NullCommand)(implicit bus: EventBus): List[Event[_]] = List()
  override val eh: (Event[_]) => Actor[NullCommand, NullActor] = ev => this
}



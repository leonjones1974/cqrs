package uk.camsw.cqrs

class StringEventHandler extends EventHandler[StringEventHandler] {
  var received = List.empty[String]

  override def onEvent: (Event[_]) => StringEventHandler = e => e.data match {
    case s: String =>
      received = received :+ s
      this
    case _ => this
  }
}

class IntEventHandler extends EventHandler[IntEventHandler] {
  var received = List.empty[Int]

  override def onEvent: (Event[_]) => IntEventHandler = e => e.data match {
    case n: Int =>
      received = received :+ n
      this
    case s => this
  }
}

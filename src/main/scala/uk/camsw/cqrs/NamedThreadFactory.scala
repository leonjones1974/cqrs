package uk.camsw.cqrs

import java.util.concurrent.{Executors, ThreadFactory}

case class NamedThreadFactory(name: String) extends ThreadFactory {
  val defaultFactory = Executors.defaultThreadFactory()

  override def newThread(r: Runnable): Thread = updateName(defaultFactory.newThread(r))

  val lastHyphenIndex: String => Int =
    s => s.lastIndexOf("-")

  val hasIndex: String => Boolean =
    s => lastHyphenIndex(s) != -1

  val buildName: Thread => String =
    t => {
      val defaultName = t.getName
      if (hasIndex(defaultName)) name + defaultName.substring(lastHyphenIndex(defaultName))
      else defaultName
    }

  val updateName: Thread => Thread =
    t => {
      t.setName(buildName(t))
      t
    }
}
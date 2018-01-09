package uk.camsw.cqrs.journal

import org.joda.time.LocalDateTime
import uk.camsw.cqrs.Event

import scala.reflect.io.File

object Events {

  case class JournalRehydrationStarted(data: File) extends Event[File]
  case class JournalRehydrationFinished(data: File) extends Event[File]
  case class JournallingSuspended(data: LocalDateTime) extends Event[LocalDateTime]
}

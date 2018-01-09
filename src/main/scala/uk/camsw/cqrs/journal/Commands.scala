package uk.camsw.cqrs.journal

import uk.camsw.cqrs.DomainCommand

import scala.reflect.io.File

object Commands {
  trait JournalDomainCommand extends DomainCommand
  case class RehydrateJournal(file: File) extends JournalDomainCommand
  object SuspendJournalling extends JournalDomainCommand
}

package uk.camsw.cqrs.journal

import java.util.UUID

import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import uk.camsw.cqrs._
import uk.camsw.cqrs.journal.Commands.{JournalDomainCommand, RehydrateJournal, SuspendJournalling}
import uk.camsw.cqrs.journal.Events.{JournalRehydrationFinished, JournalRehydrationStarted}
import uk.camsw.cqrs.test.DomainTestSupport

import scala.reflect.classTag
import scala.reflect.io.File

class JournalDomainTest extends FunSpec with Matchers
  with DomainTestSupport[JournalDomainCommand, JournalDomain]
  with BeforeAndAfter {

  import JournalDomainTest._

  override val commandTag = classTag[JournalDomainCommand]
  var journalFile: File = _

  after {
    if (journalFile.exists) journalFile.parent.parent.deleteRecursively()
  }

  override def domainUnderTest(): Domain[JournalDomainCommand, JournalDomain] = {
    journalFile = File(s"/tmp/.edge2_unit/${UUID.randomUUID()}/journal/edge2.journal")
    val journalDir = journalFile.parent
    journalDir.createDirectory(force = true, failIfExists = false)
    JournalDomain(journalFile)
  }

  describe("Journal Domain") {
    it("should persist persistent events and be able to rehydrate them") {
      for {e <- persistentEvents} domain << e
      for {e <- transientEvents} domain << e
      domain << RehydrateJournal(File(journalFile))
      domain.raisedEvents should contain(JournalRehydrationStarted(journalFile))
      domain.raisedEvents should contain inOrder(persistentEvents.head, persistentEvents.tail.head, persistentEvents.tail.tail: _*)
      domain.raisedEvents should contain(JournalRehydrationFinished(journalFile))
    }

    it("should rehydrate nothing where the file does not exist") {
      domain << RehydrateJournal(File(journalFile))
      domain.raisedEvents should contain only(JournalRehydrationStarted(journalFile), JournalRehydrationFinished(journalFile))
    }

    it("should not re-persist events during hydration") {
      for {e <- persistentEvents} domain << e
      domain << RehydrateJournal(File(journalFile))
      domain << RehydrateJournal(File(journalFile))
      domain << RehydrateJournal(File(journalFile))
      domain.raisedEvents.size shouldBe (persistentEvents.size * 3) + 6 // Bookends
    }

    it("should stop persisting events following a pause command") {
      given {
        domain << persistentEvents.head
        domain << SuspendJournalling
        domain << persistentEvents(1)
      }
      domain << RehydrateJournal(journalFile)
      domain.raisedEvents should contain only(
        JournalRehydrationStarted(journalFile),
        persistentEvents.head,
        JournalRehydrationFinished(journalFile)
        )
    }
  }
}

object JournalDomainTest {

  case class PersistentEvent(data: Map[Int, String]) extends Models.PersistentEvent[Map[Int, String]]

  case class TransientEvent(data: Int = 1) extends Event[Int]

  val persistentEvents = for {
    s <- (1 to 1000).toList
  } yield PersistentEvent(Map(s -> s"Event: $s"))

  val transientEvents = for {
    s <- (1 to 1000).toList
  } yield TransientEvent()

}

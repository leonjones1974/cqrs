package uk.camsw.cqrs.journal

import java.nio.file.{Files, Paths}

import better.files.{File => ScalaFile}
import grizzled.slf4j.Logging
import org.joda.time.LocalDateTime
import sun.misc.{BASE64Decoder, BASE64Encoder}
import uk.camsw.cqrs.hospice.Commands._
import uk.camsw.cqrs.hospice.Models.HospiceEntryType.PoisonEvent
import uk.camsw.cqrs.hospice.Models.InvalidData
import uk.camsw.cqrs.journal.Commands.{JournalDomainCommand, RehydrateJournal, SuspendJournalling}
import uk.camsw.cqrs.journal.Events.{JournalRehydrationFinished, JournalRehydrationStarted, JournallingSuspended}
import uk.camsw.cqrs.journal.Models.PersistentEvent
import uk.camsw.cqrs.{Domain, EventBus}

import scala.reflect.io.File
import scala.util.{Failure, Try}

trait Serialization {

  def serialize(ev: PersistentEvent[_]): Array[Byte]
  def deserialize(bytes: Array[Byte]): List[Try[PersistentEvent[_]]]

}

case class JournalDomain(journalPath: File,
                         serialization: Serialization,
                         rehydrating: Boolean = false,
                         suspended: Boolean = false)(implicit bus: EventBus) extends Domain[JournalDomainCommand, JournalDomain]
  with Logging {

  handleCommand { case RehydrateJournal(file) if file.exists =>
    info(s"Rehydrating journal from [$file]")
    val events = JournalDomain.journalledEvents(file, serialization)
    val valid = events.collect { case x if x.isSuccess => x.get }
    val invalid = events.collect { case x if x.isFailure => x }

    invalid foreach {
      case Failure(t) => bus << RegisterInvalidData(InvalidData(PoisonEvent, t, "Unable to deserialize event in journal"))
      case _ => ()
    }

    Seq(JournalRehydrationStarted(file)) ++ valid :+ JournalRehydrationFinished(file)
  }

  handleCommand { case RehydrateJournal(file) if !file.exists =>
    warn(s"Journal: [$file] does not exist")
    JournalRehydrationStarted(file) :: JournalRehydrationFinished(file) :: Nil
  }

  handleCommand {
    case SuspendJournalling if !suspended =>
      JournallingSuspended(LocalDateTime.now)
  }

  onEvent { case JournalRehydrationStarted(f) =>
    info("Rehydration started")
    this.copy(journalPath = f, rehydrating = true)
  }

  onEvent { case JournalRehydrationFinished(f) =>
    info("Rehydration finished")
    this.copy(journalPath = f, rehydrating = false)
  }

  onEvent {
    case JournallingSuspended(when) =>
      warn(s"Journalling suspended: [$when]")
      copy(suspended = true)
  }

  onEvent { case ev if ev.isInstanceOf[PersistentEvent[_]] && !rehydrating && !suspended =>
    ScalaFile(journalPath.path).appendByteArray(serialization.serialize(ev.asInstanceOf[PersistentEvent[_]]))
    this
  }
}

object JournalDomain {

  val decoder = new BASE64Decoder()
  val encoder = new BASE64Encoder()

  def journalledEvents(file: File, serialization: Serialization) : List[Try[PersistentEvent[_]]] = {
    val bytes = Files.readAllBytes(Paths.get(file.path))
    serialization.deserialize(bytes)
  }

}




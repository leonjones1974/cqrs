package uk.camsw.cqrs.journal

import java.io.ByteArrayInputStream
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
import uk.camsw.cqrs.journal.JournalDomain._
import uk.camsw.cqrs.journal.Models.PersistentEvent
import uk.camsw.cqrs.{Domain, EventBus}

import scala.pickling._
import scala.pickling.fastbinary._
import scala.pickling.shareNothing._
import scala.reflect.io.File
import scala.util.{Failure, Try}

case class JournalDomain(journalPath: File,
                         rehydrating: Boolean = false,
                         suspended: Boolean = false)(implicit bus: EventBus) extends Domain[JournalDomainCommand, JournalDomain]
  with Logging {

  handleCommand { case RehydrateJournal(file) if file.exists =>
    info(s"Rehydrating journal from [$file]")
    val events = file.journalledEvents
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
    ScalaFile(journalPath.path).appendByteArray(serialize(ev.asInstanceOf[PersistentEvent[_]]))
    this
  }
}

object JournalDomain {

  val _keepImport = ShareNothing

  val decoder = new BASE64Decoder()
  val encoder = new BASE64Encoder()

  def serialize(ev: PersistentEvent[_]): Array[Byte] = ev.pickle.value

  def deserialize(entry: Array[Byte]): PersistentEvent[_] = BinaryPickle(entry).unpickle[PersistentEvent[_]]

  implicit class ByteArrayPimp(xs: Array[Byte]) {
    def journalledEvents = journalIterator.toList

    def journalIterator= {
      val inputStream = new ByteArrayInputStream(xs)
      new Iterator[Try[PersistentEvent[_]]] {
        val streamPickle = BinaryPickleStream(inputStream)
        override def hasNext: Boolean = {
          inputStream.available > 0
        }
        override def next(): Try[PersistentEvent[_]] = Try {streamPickle.unpickle[PersistentEvent[_]]}
      }
    }
  }

  implicit class FilePimp(file: File) {
    def journalledEvents = journalIterator.toList

    def journalIterator = Files.readAllBytes(Paths.get(file.path)).journalIterator
  }

}




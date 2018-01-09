package uk.camsw.cqrs.test

import org.joda.time.{Duration, Instant, LocalDate}
import uk.camsw.cqrs.Command

import scala.util.Random

trait CqrsDataSupport {

  private val r = Random

  def anInstant = Instant.now()
  def aDuration = Duration.standardSeconds(r.nextInt())

  def aString(): String = (r.alphanumeric take 5).toList.mkString

  def anInt(): Int = r.nextInt()

  def aLocalDate() =  LocalDate.now

  case class StringCommand(data: String) extends Command[String]
  case class IntCommand(data: Int) extends Command[Int]

}


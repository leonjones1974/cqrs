package uk.camsw.cqrs

import org.joda.time.LocalDate

import scala.util.Random

trait DataTestSupport {

  private val r = Random

  def aString(): String = (r.alphanumeric take 5).toList.mkString

  def anInt(): Int = r.nextInt()

  def aLocalDate() =  LocalDate.now

  case class StringCommand(data: String) extends Command[String]
  case class IntCommand(data: Int) extends Command[Int]

}


package uk.camsw.cqrs.hospice

import uk.camsw.cqrs.Event
import uk.camsw.cqrs.hospice.Models.InvalidData

object Events {
  case class DataHospiced(data: InvalidData) extends Event[InvalidData]
}

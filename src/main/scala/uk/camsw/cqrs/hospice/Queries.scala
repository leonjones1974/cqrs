package uk.camsw.cqrs.hospice

import java.util.UUID

import uk.camsw.cqrs.hospice.Models.InvalidData
import uk.camsw.cqrs.{QueryCommand, ViewQueried}

object Queries {

  case class HospiceQueried(data: List[InvalidData])(override val id: UUID) extends ViewQueried[InvalidData]

  trait QueryHospice extends QueryCommand {
    def execute: HospiceView => List[InvalidData]
  }

  val All = new QueryHospice {
    val execute: HospiceView => List[InvalidData] = {
      _.hospiced
    }
  }
}


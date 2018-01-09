package uk.camsw.cqrs.hospice

import grizzled.slf4j.Logging
import uk.camsw.cqrs.View
import uk.camsw.cqrs.hospice.Events.DataHospiced
import uk.camsw.cqrs.hospice.Models.InvalidData
import uk.camsw.cqrs.hospice.Queries.{HospiceQueried, QueryHospice}

case class HospiceView(hospiced: List[InvalidData] = List())
  extends View[QueryHospice, HospiceDomain] with Logging {

  handleQuery { case query: QueryHospice =>
    id => {
      val results = query.execute(this)
      HospiceQueried(results)(id)
    }
  }

  onEvent { case DataHospiced(data) => copy(hospiced = data :: hospiced) }
}
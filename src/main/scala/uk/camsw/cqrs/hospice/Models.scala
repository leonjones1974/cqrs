package uk.camsw.cqrs.hospice

object Models {
  trait HospiceEntryType

  object HospiceEntryType {
    case object PoisonEvent extends HospiceEntryType
    case object Test extends HospiceEntryType
  }

  case class InvalidData(dataType: HospiceEntryType, data: Any, reason: String)
}

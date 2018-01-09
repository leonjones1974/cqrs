package uk.camsw.cqrs.journal

import uk.camsw.cqrs.Event

object Models {
  trait PersistentEvent[A] extends Event[A]
}

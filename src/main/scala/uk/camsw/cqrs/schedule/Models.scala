package uk.camsw.cqrs.schedule

import org.joda.time.{Duration, Instant}

import scala.util.{Failure, Success, Try}

object Models {
  type TaskId = String
  type SideEffect[T] = () => Try[T]


  case class InvocationResult(task: Task[_], startTime: Instant, elapsedTime: Duration, result: Try[_]) {
    val message = result match {
      case Success(m) => m.toString
      case Failure(ex) => ex.toString
    }
  }

  case class Task[T](freq: Duration, f: SideEffect[T], id: TaskId) {
    val invoke = f

    val nextDue: Instant => Instant = last => last plus freq
  }

  object Task {
    def apply[T](f: SideEffect[T], id: TaskId) = new Task(Duration.ZERO, f, id)
  }

}

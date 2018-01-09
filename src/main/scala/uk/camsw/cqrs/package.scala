package uk.camsw

package object cqrs {
  type =>?[-A, +B] = PartialFunction[A, B]
}

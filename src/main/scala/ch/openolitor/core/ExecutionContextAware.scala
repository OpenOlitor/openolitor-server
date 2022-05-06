package ch.openolitor.core

import scala.concurrent.ExecutionContext

trait ExecutionContextAware {
  implicit protected val executionContext: ExecutionContext
}

/**
  * Copyright (C) 2016-2018 Cafienne B.V. <https://www.cafienne.io/bounded>
  */
package io.cafienne.bounded.akka.persistence.eventmaterializers

import akka.Done
import akka.persistence.query.Offset

import scala.concurrent.Future

trait Resumable {
  def registerListener(maybeStartOffset: Option[Offset]): Future[Done]
}

trait ResumableReplayable extends Resumable {
  def replayEvents(): Future[Offset]
}

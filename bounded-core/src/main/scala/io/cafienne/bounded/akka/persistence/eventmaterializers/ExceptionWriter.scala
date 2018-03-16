/**
  * Copyright (C) 2016-2018 Cafienne B.V. <https://www.cafienne.io/bounded>
  */
package io.cafienne.bounded.akka.persistence.eventmaterializers

import java.io.{PrintWriter, StringWriter}

trait ExceptionWriter {

  def logException(t: Throwable): String = {
    val sw = new StringWriter
    t.printStackTrace(new PrintWriter(sw))
    sw.toString
  }

}

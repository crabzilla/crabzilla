package io.github.crabzilla

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object Jackson {

  val json: ObjectMapper = jacksonObjectMapper().findAndRegisterModules()
}

package io.github.crabzilla.example1.impl

import io.github.crabzilla.example1.SampleInternalService
import java.time.Instant
import java.util.*

class SampleInternalServiceImpl : SampleInternalService {

  override fun uuid(): UUID {
    return UUID.randomUUID()
  }

  override fun now(): Instant {
    return Instant.now()
  }

}



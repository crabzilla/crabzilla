package crabzilla.example1.services

import crabzilla.example1.SampleInternalService
import java.time.Instant
import java.util.*

class KSampleInternalServiceImpl : SampleInternalService {

  override fun uuid(): UUID {
    return UUID.randomUUID()
  }

  override fun now(): Instant {
    return Instant.now()
  }

}

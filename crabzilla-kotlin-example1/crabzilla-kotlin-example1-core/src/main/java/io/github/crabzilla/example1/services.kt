package io.github.crabzilla.example1

import java.time.Instant
import java.util.*

interface SampleInternalService {

  fun uuid(): UUID

  fun now(): Instant

}

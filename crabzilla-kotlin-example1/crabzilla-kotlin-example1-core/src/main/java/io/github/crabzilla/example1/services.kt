package io.github.crabzilla.example1

import java.time.Instant
import java.util.*

//tag::service[]
interface SampleInternalService {

  fun uuid(): UUID
  fun now(): Instant
}
//end::service[]
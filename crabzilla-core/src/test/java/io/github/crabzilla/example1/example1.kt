package io.github.crabzilla.example1

import java.time.Instant
import java.util.*

enum class CommandHandlers {
  CUSTOMER
}


data class CustomerSummary(val id: String, val name: String, val isActive: Boolean)

// https://streamdata.io/blog/vert-x-and-the-async-calls-chain/ TODO consider to support async services

class PojoService {
  fun uuid(): UUID {
    return UUID.randomUUID()
  }
  fun now(): Instant {
    return Instant.now()
  }
}

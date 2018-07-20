package io.github.crabzilla.example1

import java.time.Instant
import java.util.*

fun subDomainName() = "example1"

enum class CommandHandlers {
  CUSTOMER
}

data class CustomerSummary(val id: String, val name: String, val isActive: Boolean)

// https://streamdata.io/blog/vert-x-and-the-async-calls-chain/ TODO consider to make these services async

class PojoService {
  fun uuid(): UUID {
    return UUID.randomUUID()
  }
  fun now(): Instant {
    return Instant.now()
  }
}

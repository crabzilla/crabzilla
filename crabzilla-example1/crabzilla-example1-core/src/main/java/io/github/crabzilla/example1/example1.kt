package io.github.crabzilla.example1

import java.time.Instant
import java.util.*

fun subDomainName() = "example1"

enum class CommandHandlers {
  CUSTOMER
}

data class CustomerSummary(val id: String, val name: String, val isActive: Boolean)

// https://streamdata.io/blog/vert-x-and-the-async-calls-chain/ TODO make these services async

interface CustomerRepository {
  fun getAll(): List<CustomerSummary>
}

interface SampleInternalService {
  fun uuid(): UUID
  fun now(): Instant
}


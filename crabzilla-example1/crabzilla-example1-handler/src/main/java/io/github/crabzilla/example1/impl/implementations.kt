package io.github.crabzilla.example1.impl

import io.github.crabzilla.example1.CustomerRepository
import io.github.crabzilla.example1.CustomerSummary
import io.github.crabzilla.example1.CustomerSummaryDao
import io.github.crabzilla.example1.SampleInternalService
import java.time.Instant
import java.util.*
import javax.inject.Inject

class CustomerRepositoryImpl @Inject constructor(private val dao: CustomerSummaryDao) : CustomerRepository {

  override fun getAll(): List<CustomerSummary> {
    return dao.getAll()
  }

}

class SampleInternalServiceImpl : SampleInternalService {

  override fun uuid(): UUID {
    return UUID.randomUUID()
  }

  override fun now(): Instant {
    return Instant.now()
  }

}

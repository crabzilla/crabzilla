package io.github.crabzilla.example1.repositories

import io.github.crabzilla.example1.CustomerRepository
import io.github.crabzilla.example1.CustomerSummary
import javax.inject.Inject

class CustomerRepositoryImpl @Inject constructor(private val dao: CustomerSummaryDao) : CustomerRepository {

  override fun getAll(): List<CustomerSummary> {
    return dao.getAll()
  }

}

package io.github.crabzilla.example1.impl

import io.github.crabzilla.example1.CustomerRepository
import io.github.crabzilla.example1.CustomerSummary
import io.github.crabzilla.example1.CustomerSummaryDao
import javax.inject.Inject

class CustomerRepositoryImpl @Inject constructor(private val dao: CustomerSummaryDao) : CustomerRepository {

  override fun getAll(): List<CustomerSummary> {
    return dao.getAll()
  }

}

package io.github.crabzilla.example1

import javax.inject.Inject

class CustomerRepositoryImpl @Inject constructor(private val dao: CustomerSummaryDao) : CustomerRepository {

  override fun getAll(): List<CustomerSummary> {
    return dao.getAll()
  }

}

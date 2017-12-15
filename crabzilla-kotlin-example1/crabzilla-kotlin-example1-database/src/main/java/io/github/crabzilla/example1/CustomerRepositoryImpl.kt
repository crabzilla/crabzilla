package io.github.crabzilla.example1

class CustomerRepositoryImpl(private val dao: CustomerSummaryDao) : CustomerRepository {

  override fun getAll(): List<CustomerSummary> {
    return dao.getAll()
  }

}

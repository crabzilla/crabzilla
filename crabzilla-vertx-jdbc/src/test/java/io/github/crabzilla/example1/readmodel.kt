package io.github.crabzilla.example1

//tag::readmodel[]

data class CustomerSummary(val id: String, val name: String, val isActive: Boolean)

interface CustomerRepository {

  fun getAll(): List<CustomerSummary>
}
//end::readmodel[]
package crabzilla.example1

data class CustomerSummary(val id: String, val name: String, val isActive: Boolean)

interface CustomerRepository {

  fun getAll(): List<CustomerSummary>

}
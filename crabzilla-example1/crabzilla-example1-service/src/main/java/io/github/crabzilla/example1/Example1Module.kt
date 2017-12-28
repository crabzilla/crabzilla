package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import io.github.crabzilla.example1.customer.CustomerModule
import io.github.crabzilla.example1.repositories.CustomerRepositoryImpl
import io.github.crabzilla.example1.repositories.CustomerSummaryDao
import io.github.crabzilla.example1.services.SampleInternalServiceImpl
import io.github.crabzilla.vertx.modules.CrabzillaModule
import io.github.crabzilla.vertx.modules.qualifiers.ReadDatabase
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.jdbi.v3.core.Jdbi
import javax.inject.Singleton


// tag::module[]
@Module(includes = [CustomerModule::class])
class Example1Module(vertx: Vertx, config: JsonObject) : CrabzillaModule(vertx, config) {

  @Provides
  @Singleton
  fun customerSummaryDao(@ReadDatabase jdbi: Jdbi) : CustomerSummaryDao {
    return jdbi.onDemand(CustomerSummaryDao::class.java)
  }

  @Provides
  @Singleton
  fun customerRepository(dao: CustomerSummaryDao): CustomerRepository {
    return CustomerRepositoryImpl(dao)
  }

  @Provides
  @Singleton
  fun service(): SampleInternalService {
    return SampleInternalServiceImpl()
  }

}
// end::module[]
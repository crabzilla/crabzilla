package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import io.github.crabzilla.example1.customer.CustomerModule
import io.github.crabzilla.example1.repositories.CustomerSummaryDao
import io.github.crabzilla.example1.services.SampleInternalServiceImpl
import io.github.crabzilla.vertx.modules.qualifiers.ReadDatabase
import org.jdbi.v3.core.Jdbi
import javax.inject.Singleton


// tag::module[]
/**
 * This command handler module exposes DAOs for your repositories and also services implementations.
 */
@Module(includes = [CustomerModule::class])
class HandlerModule {

  @Provides
  @Singleton
  fun customerSummaryDao(@ReadDatabase jdbi: Jdbi) : CustomerSummaryDao {
    return jdbi.onDemand(CustomerSummaryDao::class.java)
  }


  @Provides
  @Singleton
  fun service(): SampleInternalService {
    return SampleInternalServiceImpl()
  }

}
// end::module[]
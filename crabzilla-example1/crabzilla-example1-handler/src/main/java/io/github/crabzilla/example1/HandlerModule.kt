package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import io.github.crabzilla.example1.customer.CustomerModule
import io.github.crabzilla.example1.impl.SampleInternalServiceImpl
import io.github.crabzilla.vertx.ReadDatabase
import io.github.crabzilla.vertx.UnitOfWorkRepository
import io.github.crabzilla.vertx.WriteDatabase
import io.github.crabzilla.vertx.impl.UnitOfWorkRepositoryImpl
import io.vertx.ext.jdbc.JDBCClient
import org.jdbi.v3.core.Jdbi
import javax.inject.Singleton


// tag::module[]
/**
 * This command handler module exposes DAOs, repositories and also services implementations.
 */
@Module(includes = [(CustomerModule::class)])
class HandlerModule {

  @Provides
  @Singleton
  fun uowRepo(@WriteDatabase jdbcClient: JDBCClient): UnitOfWorkRepository {
    return UnitOfWorkRepositoryImpl(jdbcClient)
  }

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

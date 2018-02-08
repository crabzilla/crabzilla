package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import io.github.crabzilla.example1.customer.CustomerModule
import io.github.crabzilla.example1.impl.SampleInternalServiceImpl
import io.github.crabzilla.vertx.entity.EntityUnitOfWorkRepository
import io.github.crabzilla.vertx.entity.impl.EntityUnitOfWorkRepositoryImpl
import io.github.crabzilla.vertx.modules.qualifiers.ReadDatabase
import io.github.crabzilla.vertx.modules.qualifiers.WriteDatabase
import io.vertx.ext.jdbc.JDBCClient
import org.jdbi.v3.core.Jdbi
import javax.inject.Singleton


// tag::module[]
/**
 * This command handler module exposes DAOs, repositories and also services implementations.
 */
@Module(includes = [CustomerModule::class])
class HandlerModule {

  @Provides
  @Singleton
  fun uowRepo(@WriteDatabase jdbcClient: JDBCClient): EntityUnitOfWorkRepository {
    return EntityUnitOfWorkRepositoryImpl(jdbcClient)
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

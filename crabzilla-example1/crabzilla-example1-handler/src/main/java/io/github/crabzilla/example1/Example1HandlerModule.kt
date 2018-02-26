package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import io.github.crabzilla.example1.customer.CustomerModule
import io.github.crabzilla.example1.impl.SampleInternalServiceImpl
import io.github.crabzilla.vertx.UnitOfWorkRepository
import io.github.crabzilla.vertx.WriteDatabase
import io.github.crabzilla.vertx.impl.UnitOfWorkRepositoryImpl
import io.vertx.ext.jdbc.JDBCClient
import javax.inject.Singleton


// tag::module[]
/**
 * This command handler module exposes UnitOfWorkRepository and services implementations.
 */
@Module(includes = [(CustomerModule::class)])
class Example1HandlerModule {

  @Provides
  @Singleton
  fun service(): SampleInternalService {
    return SampleInternalServiceImpl()
  }

}
// end::module[]

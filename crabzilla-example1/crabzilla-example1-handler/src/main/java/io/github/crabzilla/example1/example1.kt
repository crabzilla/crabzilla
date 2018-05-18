package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import io.github.crabzilla.example1.customer.CustomerModule
import io.github.crabzilla.example1.impl.SampleInternalServiceImpl
import io.github.crabzilla.vertx.modules.JdbiDbReadModule
import org.jdbi.v3.sqlobject.statement.SqlQuery
import javax.inject.Singleton

// tag::dao[]
interface CustomerSummaryDao {

  @SqlQuery("select id, name, is_active from customer_summary")
  fun getAll(): List<CustomerSummary>

}

// tag::module[]
@Module(includes = [JdbiDbReadModule::class, CustomerModule::class])
class Example1HandlerModule {

  @Provides
  @Singleton
  fun service(): SampleInternalService {
    return SampleInternalServiceImpl()
  }

}

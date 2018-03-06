package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import io.github.crabzilla.example1.customer.CustomerModule
import io.github.crabzilla.example1.impl.SampleInternalServiceImpl
import javax.inject.Singleton


// tag::module[]
@Module(includes = [CustomerModule::class])
class Example1HandlerModule {

  @Provides
  @Singleton
  fun service(): SampleInternalService {
    return SampleInternalServiceImpl()
  }

}
// end::module[]

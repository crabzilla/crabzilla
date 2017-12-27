package io.github.crabzilla.example1

import dagger.Component
import io.github.crabzilla.example1.customer.CustomerModule
import io.github.crabzilla.vertx.modules.QueryDbModule
import io.github.crabzilla.vertx.modules.WriteDbModule
import io.vertx.core.Verticle
import javax.inject.Singleton

@Singleton
@Component(modules = [(WriteDbModule::class), (QueryDbModule::class), (Example1Module::class), (CustomerModule::class)])
interface Example1Component {

//  fun verticles() : Map<String, Verticle>
//
//  fun jdbi(): Jdbi
//
//  fun eventProjector(): EventProjector<CustomerSummaryDao>

}


package io.github.crabzilla.example1

import dagger.Component
import io.github.crabzilla.example1.customer.CustomerModule
import io.vertx.core.Verticle
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(CrabzillaModule::class, Example1Module::class, CustomerModule::class))
interface Example1Component {

  fun verticles() : Map<String, Verticle>

}


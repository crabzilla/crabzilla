package io.github.crabzilla.vertx.modules.test

import dagger.Component
import io.vertx.ext.healthchecks.HealthCheckHandler
import javax.inject.Singleton

@Singleton
@Component(modules = [TestModule::class])
interface TestComponent {

  fun healthCheckHandler() : HealthCheckHandler

}

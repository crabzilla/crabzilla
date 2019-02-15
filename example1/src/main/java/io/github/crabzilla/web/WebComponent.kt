package io.github.crabzilla.web

import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [WebModule::class, PgClientModule::class])
interface WebComponent {

//  fun healthHandlers() : Map<String, Handler<Future<Status>>>

}


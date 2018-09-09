package io.github.crabzilla.web

import dagger.Component
import io.github.crabzilla.pgclient.PgClientModule
import javax.inject.Singleton

@Singleton
@Component(modules = [WebModule::class, PgClientModule::class])
interface WebComponent {

//  fun healthHandlers() : Map<String, Handler<Future<Status>>>

}


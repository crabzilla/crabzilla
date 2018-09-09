package io.github.crabzilla.vertx

import javax.inject.Qualifier

@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class ReadDatabase

@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class WriteDatabase

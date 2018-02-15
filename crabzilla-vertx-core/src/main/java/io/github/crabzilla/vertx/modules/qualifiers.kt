package io.github.crabzilla.vertx.modules

import java.lang.annotation.Documented
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import javax.inject.Qualifier

// databases

@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
annotation class ReadDatabase

@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
annotation class WriteDatabase

@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
annotation class ProjectionDatabase

// health checks

@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
annotation class WebHealthCheck

@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
annotation class HandlerHealthCheck

@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
annotation class ProjectorHealthCheck


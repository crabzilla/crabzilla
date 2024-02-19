package io.github.crabzilla.context

open class CrabzillaRuntimeException(override val message: String, override val cause: Throwable? = null) :
  RuntimeException(message, cause)

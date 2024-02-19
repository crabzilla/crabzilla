package io.github.crabzilla.command

import io.github.crabzilla.context.CrabzillaRuntimeException

class BusinessException(message: String, cause: Throwable) : CrabzillaRuntimeException(message, cause)

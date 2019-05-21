package io.github.crabzilla

interface EntityCommandFunctions<E: Entity> {

  fun validateCmd(command: Command): List<String>

  fun cmdHandlerFactory(): CommandHandlerFactory<E>

}

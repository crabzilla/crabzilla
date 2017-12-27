package io.github.crabzilla.core.entity

import io.github.crabzilla.core.Command

interface EntityCommand : Command {

  val targetId: EntityId

}

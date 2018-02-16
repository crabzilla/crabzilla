package io.github.crabzilla.core.entity

import io.github.crabzilla.core.EntityId
import java.io.Serializable

interface Entity : Serializable {

  val id: EntityId

}

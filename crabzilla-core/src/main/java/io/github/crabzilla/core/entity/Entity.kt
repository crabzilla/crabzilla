package io.github.crabzilla.core.entity

import java.io.Serializable

interface Entity : Serializable {

  val id: EntityId

}
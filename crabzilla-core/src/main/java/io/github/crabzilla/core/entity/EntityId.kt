package io.github.crabzilla.core.entity

import com.fasterxml.jackson.annotation.JsonTypeInfo

import java.io.Serializable

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
interface EntityId : Serializable {
  fun stringValue(): String
}

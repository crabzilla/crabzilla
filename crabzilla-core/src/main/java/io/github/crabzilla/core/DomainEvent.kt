package io.github.crabzilla.core

import com.fasterxml.jackson.annotation.JsonTypeInfo

import java.io.Serializable

/**
 * A DomainEvent interface.
 *
 * The JsonTypeInfo annotation enables a polymorphic JSON serialization for your events.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
interface DomainEvent : Serializable

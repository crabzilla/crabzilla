package io.github.crabzilla.core

import com.fasterxml.jackson.annotation.JsonTypeInfo

import java.io.Serializable

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
interface DomainEvent : Serializable

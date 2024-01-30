package io.github.crabzilla.example1.customer

import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import io.github.crabzilla.kotlinx.json.javaModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@kotlinx.serialization.ExperimentalSerializationApi
val customerModule =
  SerializersModule {
    include(javaModule)
    polymorphic(CustomerCommand::class) {
      subclass(RegisterCustomer::class, RegisterCustomer.serializer())
      subclass(ActivateCustomer::class, ActivateCustomer.serializer())
      subclass(DeactivateCustomer::class, DeactivateCustomer.serializer())
      subclass(RegisterAndActivateCustomer::class, RegisterAndActivateCustomer.serializer())
    }
    polymorphic(CustomerEvent::class) {
      subclass(CustomerRegistered::class, CustomerRegistered.serializer())
      subclass(CustomerActivated::class, CustomerActivated.serializer())
      subclass(CustomerDeactivated::class, CustomerDeactivated.serializer())
    }
  }

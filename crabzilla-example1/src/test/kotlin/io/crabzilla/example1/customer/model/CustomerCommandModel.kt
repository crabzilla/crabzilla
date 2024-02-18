package io.crabzilla.example1.customer.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.crabzilla.core.buildException
import io.crabzilla.example1.customer.model.CustomerCommand.ActivateCustomer
import io.crabzilla.example1.customer.model.CustomerCommand.DeactivateCustomer
import io.crabzilla.example1.customer.model.CustomerCommand.RegisterAndActivateCustomer
import io.crabzilla.example1.customer.model.CustomerCommand.RegisterCustomer
import io.crabzilla.example1.customer.model.CustomerCommand.RenameCustomer
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes(
  JsonSubTypes.Type(RegisterCustomer::class, name = "RegisterCustomer"),
  JsonSubTypes.Type(ActivateCustomer::class, name = "ActivateCustomer"),
  JsonSubTypes.Type(DeactivateCustomer::class, name = "DeactivateCustomer"),
  JsonSubTypes.Type(RenameCustomer::class, name = "RenameCustomer"),
  JsonSubTypes.Type(RegisterAndActivateCustomer::class, name = "RegisterAndActivateCustomer"),
)
sealed interface CustomerCommand {
  data class RegisterCustomer(val customerId: UUID, val name: String) : CustomerCommand

  data class RenameCustomer(val name: String) : CustomerCommand

  data class ActivateCustomer(val reason: String) : CustomerCommand

  data class DeactivateCustomer(val reason: String) : CustomerCommand

  data class RegisterAndActivateCustomer(
    val customerId: UUID,
    val name: String,
    val reason: String,
  ) : CustomerCommand
}

val customerDecideFunction: (state: Customer, command: CustomerCommand) -> List<CustomerEvent> = { state, command ->
  when (state) {
    is Customer.Initial -> {
      when (command) {
        is RegisterCustomer ->
          Customer.Initial.register(id = command.customerId, name = command.name)
        is RegisterAndActivateCustomer ->
          Customer.Initial.registerAndActivate(id = command.customerId, name = command.name, reason = command.reason)
        else -> throw buildException(state, command)
      }
    }
    is Customer.Active -> {
      when (command) {
        is DeactivateCustomer -> state.deactivate(reason = command.reason)
        is RenameCustomer -> state.rename(command.name)
        else -> throw buildException(state, command)
      }
    }
    is Customer.Inactive -> {
      when (command) {
        is ActivateCustomer -> {
          state.activate(reason = command.reason)
        }
        is RenameCustomer -> state.rename(command.name)
        else -> throw buildException(state, command)
      }
    }
  }
}

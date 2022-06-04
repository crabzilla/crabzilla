package io.github.crabzilla.stack.command

import io.github.crabzilla.core.FeatureComponent
import io.github.crabzilla.stack.JsonObjectSerDer

interface CommandServiceApiFactory {

  // TODO S, C and E could have public and private data (lgpd)
  // Public and private Event (base interfaces) - privacy at domain event level
  // event should have a reference to a safe wallet containing a key for crypto
  // once an authority (customer) asks to delete its data, just remove the authority key
  // when CommandServiceApi calls eventhandler function, it should know if the event is anonymized.
  //       in this case it just return the state as is (with private properties as null)

  fun <S : Any, C : Any, E : Any> commandService(
    component: FeatureComponent<S, C, E>,
    jsonObjectSerDer: JsonObjectSerDer<S, C, E>,
    options: CommandServiceOptions = CommandServiceOptions(),
  ): CommandServiceApi<C>

}

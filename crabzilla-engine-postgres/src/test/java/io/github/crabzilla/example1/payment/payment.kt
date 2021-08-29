package io.github.crabzilla.example1.payment

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.core.command.EventHandler
import io.github.crabzilla.core.command.StatefulSession
import io.github.crabzilla.example1.payment.PaymentCommand.Pay
import io.github.crabzilla.example1.payment.PaymentCommand.Refund
import io.github.crabzilla.example1.payment.PaymentEvent.PaymentApproved
import io.github.crabzilla.example1.payment.PaymentEvent.PaymentNotApproved
import io.github.crabzilla.example1.payment.PaymentEvent.PaymentRefunded
import io.github.crabzilla.example1.payment.PaymentEvent.PaymentRequested
import io.github.crabzilla.stack.command.FutureCommandHandler
import io.vertx.core.Future
import io.vertx.core.eventbus.EventBus
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import java.util.UUID

@Serializable
sealed class PaymentEvent : Event {
  @Serializable
  @SerialName("PaymentRequested")
  data class PaymentRequested(
    @Contextual val id: UUID,
    val creditCardNo: String,
    val amount: Double,
  ) : PaymentEvent()

  @Serializable
  @SerialName("PaymentApproved")
  data class PaymentApproved(val reason: String) : PaymentEvent()

  @Serializable
  @SerialName("PaymentNotApproved")
  data class PaymentNotApproved(val reason: String) : PaymentEvent()

  @Serializable
  @SerialName("PaymentRefunded")
  data class PaymentRefunded(val reason: String, val amount: Double) : PaymentEvent()
}

@Serializable
sealed class PaymentCommand : Command {
  @Serializable
  @SerialName("Pay")
  data class Pay(@Contextual val id: UUID, val creditCardNo: String, val amount: Double) :
    PaymentCommand()

  @Serializable
  @SerialName("Refund")
  data class Refund(val reason: String) : PaymentCommand()
}

@Serializable
enum class Status {
  Approved,
  NotApproved
}

@Serializable
@SerialName("Payment")
data class Payment(
  @Contextual val id: UUID,
  val creditCardNo: String,
  val amount: Double,
  val status: Status? = null,
  val reason: String? = null,
) : State {

  companion object {
    fun create(id: UUID, creditCardNo: String, amount: Double): List<PaymentEvent> {
      return listOf(PaymentRequested(id, creditCardNo, amount))
    }
    fun fromEvent(event: PaymentRequested): Payment {
      return Payment(id = event.id, creditCardNo = event.creditCardNo, amount = event.amount)
    }
  }
}

val paymentEventHandler = EventHandler<Payment, PaymentEvent> { state, event ->
  when (event) {
    is PaymentRequested -> Payment.fromEvent(event)
    is PaymentApproved -> state!!.copy(status = Status.Approved, reason = event.reason)
    is PaymentNotApproved -> state!!.copy(status = Status.NotApproved, reason = event.reason)
    is PaymentRefunded -> TODO()
  }
}

class FuturePaymentCommandHandler(handler: EventHandler<Payment, PaymentEvent>, val eventbus: EventBus) :
  FutureCommandHandler<Payment, PaymentCommand, PaymentEvent>(handler) {

  override fun handleCommand(
    command: PaymentCommand,
    state: Payment?,
  ): Future<StatefulSession<Payment, PaymentEvent>> {

    return when (command) {
      is Pay -> {
        withNew(Payment.create(command.id, command.creditCardNo, command.amount))
          .toFuture()
          .compose { s ->
            // here we could use event bus to request some external api call
            s.register(PaymentApproved("ok"))
              .toFuture()
          }
      }
      is Refund -> {
        TODO()
      }
    }
  }
}

@kotlinx.serialization.ExperimentalSerializationApi
val paymentModule = SerializersModule {
  polymorphic(State::class) {
    subclass(Payment::class, Payment.serializer())
  }
  polymorphic(Command::class) {
    subclass(Pay::class, Pay.serializer())
    subclass(Refund::class, Refund.serializer())
  }
  polymorphic(Event::class) {
    subclass(PaymentRequested::class, PaymentRequested.serializer())
    subclass(PaymentApproved::class, PaymentApproved.serializer())
    subclass(PaymentNotApproved::class, PaymentNotApproved.serializer())
    subclass(PaymentRefunded::class, PaymentRefunded.serializer())
  }
}

package io.github.crabzilla.example1.payment

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandHandlerApi.ConstructorResult
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.DomainState
import io.github.crabzilla.core.EventHandler
import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.core.StatefulSession
import io.github.crabzilla.example1.payment.PaymentCommand.Pay
import io.github.crabzilla.example1.payment.PaymentCommand.Refund
import io.github.crabzilla.example1.payment.PaymentEvent.PaymentApproved
import io.github.crabzilla.example1.payment.PaymentEvent.PaymentNotApproved
import io.github.crabzilla.example1.payment.PaymentEvent.PaymentRefunded
import io.github.crabzilla.example1.payment.PaymentEvent.PaymentRequested
import io.github.crabzilla.stack.command.CommandControllerConfig
import io.github.crabzilla.stack.command.ExternalCommandHandler
import io.vertx.core.Future
import io.vertx.core.eventbus.EventBus
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import java.math.BigDecimal
import java.util.UUID

@Serializable
sealed class PaymentEvent : DomainEvent() {
  @Serializable
  @SerialName("PaymentRequested")
  data class PaymentRequested(
    @Contextual val id: UUID,
    val creditCardNo: String,
    @Contextual val amount: BigDecimal,
  ) : PaymentEvent()

  @Serializable
  @SerialName("PaymentApproved")
  data class PaymentApproved(val reason: String) : PaymentEvent()

  @Serializable
  @SerialName("PaymentNotApproved")
  data class PaymentNotApproved(val reason: String) : PaymentEvent()

  @Serializable
  @SerialName("PaymentRefunded")
  data class PaymentRefunded(val reason: String, @Contextual val amount: BigDecimal) : PaymentEvent()
}

@Serializable
sealed class PaymentCommand : Command() {
  @Serializable
  @SerialName("Pay")
  data class Pay(@Contextual val id: UUID, val creditCardNo: String, @Contextual val amount: BigDecimal) :
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
  @Contextual val amount: BigDecimal,
  val status: Status? = null,
  val reason: String? = null,
) : DomainState() {

  companion object {
    fun create(id: UUID, creditCardNo: String, amount: BigDecimal): ConstructorResult<Payment, PaymentEvent> {
      return ConstructorResult(Payment(id, creditCardNo, amount), PaymentRequested(id, creditCardNo, amount))
    }
  }
}

val paymentEventHandler = EventHandler<Payment, PaymentEvent> { state, event ->
  when (event) {
    is PaymentRequested -> Payment.create(event.id, event.creditCardNo, event.amount).state
    is PaymentApproved -> state!!.copy(status = Status.Approved, reason = event.reason)
    is PaymentNotApproved -> state!!.copy(status = Status.NotApproved, reason = event.reason)
    is PaymentRefunded -> TODO()
  }
}

object ExternalPaymentCommandHandler : ExternalCommandHandler<Payment, PaymentCommand, PaymentEvent> {

  override fun handleCommand(
    eventBus: EventBus,
    command: PaymentCommand,
    eventHandler: EventHandler<Payment, PaymentEvent>,
    snapshot: Snapshot<Payment>?,
  ): Future<StatefulSession<Payment, PaymentEvent>> {

    return when (command) {
      is Pay -> {
        withNew(Payment.create(command.id, command.creditCardNo, command.amount), eventHandler)
          .toFuture()
          .compose { s ->
            // here we could use event bus to request some external api call
            s.register(PaymentApproved("ok")).toFuture()
          }
      }
      is Refund -> {
        TODO()
      }
    }
  }
}

val paymentConfig = CommandControllerConfig(
  "Payment",
  paymentEventHandler,
  ExternalPaymentCommandHandler
)

@kotlinx.serialization.ExperimentalSerializationApi
val paymentModule = SerializersModule {
  polymorphic(DomainState::class) {
    subclass(Payment::class, Payment.serializer())
  }
  polymorphic(Command::class) {
    subclass(Pay::class, Pay.serializer())
    subclass(Refund::class, Refund.serializer())
  }
  polymorphic(DomainEvent::class) {
    subclass(PaymentRequested::class, PaymentRequested.serializer())
    subclass(PaymentApproved::class, PaymentApproved.serializer())
    subclass(PaymentNotApproved::class, PaymentNotApproved.serializer())
    subclass(PaymentRefunded::class, PaymentRefunded.serializer())
  }
}

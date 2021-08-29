package io.github.crabzilla.example1.payment

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.core.command.EventHandler
import io.github.crabzilla.example1.payment.PaymentEvent.PaymentApproved
import io.github.crabzilla.example1.payment.PaymentEvent.PaymentNotApproved
import io.github.crabzilla.example1.payment.PaymentEvent.PaymentRefunded
import io.github.crabzilla.example1.payment.PaymentEvent.PaymentRequested
import java.util.UUID

sealed class PaymentEvent : Event {
  data class PaymentRequested(
    val id: UUID,
    val creditCardNo: String,
    val amount: Double,
  ) : PaymentEvent()

  data class PaymentApproved(val reason: String) : PaymentEvent()

  data class PaymentNotApproved(val reason: String) : PaymentEvent()

  data class PaymentRefunded(val reason: String, val amount: Double) : PaymentEvent()
}

sealed class PaymentCommand : Command {
  data class Pay(val id: UUID, val creditCardNo: String, val amount: Double) :
    PaymentCommand()

  data class Refund(val reason: String) : PaymentCommand()
}

enum class Status {
  Approved,
  NotApproved
}

data class Payment(
  val id: UUID,
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

// class FuturePaymentCommandHandler(handler: EventHandler<Payment, PaymentEvent>, val eventbus: EventBus) :
//  FutureCommandHandler<Payment, PaymentCommand, PaymentEvent>(handler) {
//
//  override fun handleCommand(
//    command: PaymentCommand,
//    snapshot: Snapshot<Payment>?,
//  ): Future<CommandSession<Payment, PaymentEvent>> {
//
//    return when (command) {
//      is Pay -> {
//        withNew(Payment.create(command.id, command.creditCardNo, command.amount))
//          .toFuture()
//          .compose { s ->
//            // here we could use event bus to request some external api call
//            s.register(PaymentApproved("ok"))
//              .toFuture()
//          }
//      }
//      is Refund -> {
//        TODO()
//      }
//    }
//  }
// }

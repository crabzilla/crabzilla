package io.github.crabzilla.web.example1

import io.github.crabzilla.*
import io.github.crabzilla.example1.*
import io.vertx.core.*
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

fun main() {
  Launcher.executeCommand("run", ComposingVerticle::class.java.name)
}

class ComposingVerticle: AbstractVerticle() {

  override fun start() {

//    command_handler_2_method2(PrintResultHandler())

    val createCmd = CreateCustomer(UUID.randomUUID(), CustomerId(1), "c1")
    val seedValue1 = CustomerAsync()
//    cmdHandlerAsync(createCmd, Snapshot(seedValue1, 0), PrintResultHandler())
//
//    val activateCmd = ActivateCustomer(UUID.randomUUID(), CustomerId(1), "I want it")
//    val seedValue2 = CustomerAsync(CustomerId(1), "c", pojoService = PojoService())
//    cmdHandlerAsync(activateCmd, Snapshot(seedValue2, 0), PrintResultHandler())

    val createActivateCmd = CreateActivateCustomer(UUID.randomUUID(), CustomerId(1),"c1", "I want it")
    cmdHandlerAsync(createActivateCmd, Snapshot(seedValue1, 0), PrintResultHandler())

    stop()


  }

  fun cmdHandlerAsync(cmd: Command, s: Snapshot<CustomerAsync>, aHandler: Handler<AsyncResult<UnitOfWork>>) {

    val uowFuture: Future<UnitOfWork> = Future.future()
    uowFuture.setHandler(aHandler)
    val customer = s.instance
    val eventsFuture: Future<List<DomainEvent>> = Future.future()
    eventsFuture.setHandler { event ->
      if (event.succeeded()) {
        uowFuture.complete(UnitOfWork.of(cmd, event.result(), s.version))
      } else {
        uowFuture.fail(event.cause())
      }
    }

    when (cmd) {

      is CreateCustomer -> customer.create(cmd.targetId, cmd.name, eventsFuture)
      is ActivateCustomer -> eventsFuture.complete(customer.activate(cmd.reason))
      is DeactivateCustomer -> customer.deactivate(cmd.reason, eventsFuture)
      is CreateActivateCustomer -> {
        val createFuture: Future<List<DomainEvent>> = Future.future()
        val tracker = StateTransitionsTracker(s, CUSTOMER_STATE_BUILDER_ASYNC)
        tracker.currentState().create(cmd.targetId, cmd.name, createFuture)
        createFuture
          .compose { v ->
            println("after create")
            println("  events $v")
            tracker.applyEvents(v)
            println("  state " + tracker.currentState())
            val activateFuture: Future<List<DomainEvent>> = Future.future()
            tracker.currentState().activate("I can", activateFuture.completer())
            activateFuture
          }
          .compose { v ->
            println("after activate")
            println("  events $v")
            tracker.applyEvents(v)
            println("  state " + tracker.currentState())
            Future.succeededFuture(tracker.collectEvents())
          }
          .compose({ v ->
            println("after collect all events")
            println("  events $v")
          }, eventsFuture)
     }
      else -> uowFuture.fail(IllegalArgumentException("$cmd.javaClass.name is a unknown command"))
    }

  }

  fun command_handler_2_method2(aHandler: Handler<AsyncResult<List<DomainEvent>>>) {

    val future: Future<List<DomainEvent>> = Future.future()
    future.setHandler(aHandler)

    val cust0 = CustomerAsync(null, null, false, null)
    val snapshot0 = Snapshot(cust0, 0)
    val tracker = StateTransitionsTracker(snapshot0, CUSTOMER_STATE_BUILDER_ASYNC)

    println("before create: " + tracker.currentState())

    val createFuture: Future<List<DomainEvent>> = Future.future()
    tracker.currentState().create(CustomerId(1), "cust-1", createFuture)

    createFuture
        .compose { v ->
          println(v)
          println("after create: " + tracker.currentState())
          val activateFuture: Future<List<DomainEvent>> = Future.future()
          tracker.applyEvents(v)
          tracker.currentState().activate("I can", activateFuture.completer())
          activateFuture
        }
        .compose { v ->
          println(v)
          println("after activate: " + tracker.currentState())
          val deActivateFuture: Future<List<DomainEvent>> = Future.future()
          tracker.applyEvents(v)
          tracker.currentState().deactivate("I can do it too", deActivateFuture.completer())
          deActivateFuture
        }
        .compose({ v ->
          println(v)
          println("after deactivate: " + tracker.currentState())
          tracker.applyEvents(v).collectEvents()
         }, future)
    }

  val CUSTOMER_STATE_BUILDER_ASYNC = { event: DomainEvent, customer: CustomerAsync ->
    when(event) {
      is CustomerCreated -> customer.copy(customerId = event.id, name =  event.name)
      is CustomerActivated -> customer.copy(isActive = true, reason = event.reason)
      is CustomerDeactivated -> customer.copy(isActive = false, reason = event.reason)
      else -> customer
    }}

  data class CustomerAsync(val customerId: CustomerId? = null,
                           val name: String? = null,
                           val isActive: Boolean? = false,
                           val reason: String? = null) : Entity {

    fun create(id: CustomerId, name: String, handler: Handler<AsyncResult<List<DomainEvent>>>) {
      handler.handle(Future.future { event ->
        require(this.customerId == null) { "customer already created" }
        event.complete(eventsOf(CustomerCreated(id, name)))
      })
    }

    fun activate(reason: String, handler: Handler<AsyncResult<List<DomainEvent>>>) {
      handler.handle(Future.future { event ->
        customerMustExist()
        event.complete(eventsOf(CustomerActivated(reason, Instant.now())))
      })
    }

    fun activate(reason: String) : List<DomainEvent> {
      customerMustExist()
      return eventsOf(CustomerActivated(reason, Instant.now()))
    }

    fun deactivate(reason: String, handler: Handler<AsyncResult<List<DomainEvent>>>) {
      handler.handle(Future.future { event ->
        customerMustExist()
        event.complete(eventsOf(CustomerDeactivated(reason, Instant.now())))
      })
    }

    private fun customerMustExist() {
      require(this.customerId != null) { "customer must exists" }
    }

  }

  private class PrintResultHandler<T> : Handler<AsyncResult<T>> {
    override fun handle(event: AsyncResult<T>) {
      if (event.succeeded()) println(event.result()) else event.cause().printStackTrace()
    }
  }

}

package io.github.crabzilla.web.example1

import io.github.crabzilla.*
import io.github.crabzilla.example1.*
import io.vertx.core.*

fun main() {
  Launcher.executeCommand("run", ComposingVerticle::class.java.name)
}

class ComposingVerticle: AbstractVerticle() {

  override fun start() {

    command_handler_2_method2(PrintResultHandler())

    this.stop()

  }

  fun command_handler_2_method2(aHandler: Handler<AsyncResult<List<DomainEvent>>>) {

    val future: Future<List<DomainEvent>> = Future.future()
    future.setHandler(aHandler)

    val cust0 = CustomerAsync(null, null, false, null, PojoService())
    val snapshot0 = Snapshot(cust0, 0)
    val tracker = StateTransitionsTracker(snapshot0, CUSTOMER_STATE_BUILDER_ASYNC)

    println("before create: " + tracker.currentState())

    val createFuture: Future<List<DomainEvent>> = Future.future()
    cust0.create(CustomerId(1), "cust-1", createFuture)

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
          tracker.applyEvents(v)
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
                           val reason: String? = null,
                           val pojoService: PojoService) : Entity {

    fun create(id: CustomerId, name: String, handler: Handler<AsyncResult<List<DomainEvent>>>) {
      handler.handle(Future.future { event -> run {
        require(this.customerId == null) { "customer already created" }
        event.complete(eventsOf(CustomerCreated(id, name)))
        }
      })
    }

    fun activate(reason: String, handler: Handler<AsyncResult<List<DomainEvent>>>) {
      handler.handle(Future.future { event -> run {
        customerMustExist()
        event.complete(eventsOf(CustomerActivated(reason, pojoService.now())))
        }
      })
    }

    fun deactivate(reason: String, handler: Handler<AsyncResult<List<DomainEvent>>>) {
      handler.handle(Future.future { event -> run {
        customerMustExist()
        event.complete(eventsOf(CustomerDeactivated(reason, pojoService.now())))
        }
      })
    }

    private fun customerMustExist() {
      require(this.customerId != null) { "customer must exists" }
    }

  }

  private class PrintResultHandler : Handler<AsyncResult<List<DomainEvent>>> {
    override fun handle(event: AsyncResult<List<DomainEvent>>) {
      if (event.succeeded()) println(event.result()) else event.cause().printStackTrace()
    }
  }

}

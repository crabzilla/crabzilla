package io.github.crabzilla.pgc.command

import io.github.crabzilla.core.AggregateRootSession
import io.github.crabzilla.core.CommandMetadata
import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand
import io.github.crabzilla.example1.CustomerEvent
import io.github.crabzilla.example1.customerEventHandler
import io.github.crabzilla.example1.customerJson
import io.github.crabzilla.pgc.cleanDatabase
import io.github.crabzilla.pgc.writeModelPgPool
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.PgPool
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PgcEventStoreIT {

  private lateinit var writeDb: PgPool
  private lateinit var eventStore: PgcEventStore<Customer, CustomerCommand, CustomerEvent>

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    val envOptions = ConfigStoreOptions()
      .setType("file")
      .setFormat("properties")
      .setConfig(JsonObject().put("path", "../example1.env"))
    val options = ConfigRetrieverOptions().addStore(envOptions)
    val retriever = ConfigRetriever.create(vertx, options)
    retriever.getConfig(
      Handler { configFuture ->
        if (configFuture.failed()) {
          println("Failed to get configuration")
          tc.failNow(configFuture.cause())
          return@Handler
        }
        val config = configFuture.result()
        writeDb = writeModelPgPool(vertx, config)
        eventStore = PgcEventStore(writeDb, customerJson)
        cleanDatabase(vertx, config)
          .onSuccess {
            tc.completeNow()
            println("ok")
          }
          .onFailure { err ->
            tc.failNow(err)
            err.printStackTrace()
          }
      }
    )
  }

  @Test
  @DisplayName("can append version 1")
  fun s1(tc: VertxTestContext) {
    val customer = Customer.create(id = 1, name = "c1")
    val cmd = CustomerCommand.ActivateCustomer("is needed")
    val metadata = CommandMetadata(1)
    val session = AggregateRootSession(0, customer.state, customerEventHandler)
    session.execute { it.activate(cmd.reason) }
    eventStore.append(cmd, metadata, session)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

//  @Test
//  @DisplayName("cannot append version 1 twice")
//  fun s2(tc: VertxTestContext) {
//    eventStore.append(createdUow1)
//      .onFailure { tc.failNow(it) }
//      .onSuccess {
//        eventStore.append(createdUow1)
//          .onSuccess { tc.failNow("should fail") }
//          .onFailure { err ->
//            tc.verify { assertThat(err.message).isEqualTo("expected version is 0 but current version is 1") }
//            tc.completeNow()
//          }
//      }
//  }
//
//  @Test
//  @DisplayName("cannot append version 3 after version 1")
//  fun s22(tc: VertxTestContext) {
//    val createdUow3 =
//      UnitOfWork(CUSTOMER_ENTITY, customerId1, UUID.randomUUID(), createCmd1, 3, listOf(created1))
//    // append uow1
//    eventStore.append(createdUow1)
//      .onFailure { tc.failNow(it) }
//      .onSuccess {
//        eventStore.append(createdUow3)
//          .onSuccess { tc.failNow("should fail") }
//          .onFailure { err ->
//            tc.verify { assertThat(err.message).isEqualTo("expected version is 2 but current version is 1") }
//            tc.completeNow()
//          }
//      }
//  }
//
//  @Test
//  @DisplayName("can append version 1 and version 2")
//  fun s3(tc: VertxTestContext) {
//    eventStore.append(createdUow1).onComplete { ar1 ->
//      if (ar1.failed()) {
//        tc.failNow(ar1.cause())
//      } else {
//        eventStore.append(activatedUow1).onComplete { ar2 ->
//          if (ar2.failed()) {
//            tc.failNow(ar2.cause())
//          } else {
//            // TODO check db state
//            tc.completeNow()
//          }
//        }
//      }
//    }
//  }
}

package io.github.crabzilla.cassandra

import com.datastax.oss.driver.api.core.CqlSessionBuilder
import io.github.crabzilla.core.StatefulSession
import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand
import io.github.crabzilla.example1.CustomerEvent
import io.github.crabzilla.example1.customerConfig
import io.github.crabzilla.example1.customerEventHandler
import io.github.crabzilla.stack.CommandMetadata
import io.vertx.cassandra.CassandraClient
import io.vertx.cassandra.CassandraClientOptions
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import jdk.nashorn.internal.ir.annotations.Ignore
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.atomic.AtomicBoolean

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Ignore
class CassandraEventStoreIT {

  // TODO check if appended data match

  private val created = AtomicBoolean(false)
  private lateinit var cassandraClient: CassandraClient
  private lateinit var eventStore: CassandraEventStore<Customer, CustomerCommand, CustomerEvent>

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    val options = CassandraClientOptions(CqlSessionBuilder().withLocalDatacenter("datacenter1"))
      .addContactPoint("localhost", 9042)
//            .setKeyspace("example1")
    cassandraClient = CassandraClient.createShared(vertx, "sharedClientName", options)
    eventStore = CassandraEventStore("example1", cassandraClient, customerConfig)
    if (!created.get()) {
      cassandraClient
        .execute(
          "CREATE KEYSPACE IF NOT EXISTS example1 " +
            "WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };"
        )
        .compose { cassandraClient.execute("USE example1;") }
        .compose { cassandraClient.execute("CREATE TABLE IF NOT EXISTS example1.snapshots (ar_id INT, ar_name VARCHAR, version INT, json_content VARCHAR, PRIMARY KEY (ar_id, ar_name));") }
        .compose { cassandraClient.execute("CREATE TABLE IF NOT EXISTS example1.events (event_id timeuuid, event_payload VARCHAR, ar_name VARCHAR, ar_id INT, version INT, cmd_id VARCHAR, PRIMARY KEY (event_id, ar_id, ar_name));") }
        .onSuccess { tc.completeNow() }
        .onFailure { tc.failNow(it) }
      created.set(true)
    } else {
      cassandraClient
        .execute("truncate example1.snapshots")
        .compose { cassandraClient.execute("truncate example1.events") }
        .onSuccess { tc.completeNow() }
        .onFailure { tc.failNow(it) }
    }
  }

  @Test
  @DisplayName("can append version 1")
  fun s1(tc: VertxTestContext) {
    val customer = Customer.create(id = 1, name = "c1") // AggregateRootSession needs a non null state
    val cmd = CustomerCommand.ActivateCustomer("is needed")
    val metadata = CommandMetadata(1)
    val session = StatefulSession(0, customer.state, customerEventHandler)
    session.execute { it.activate(cmd.reason) }
    eventStore.append(cmd, metadata, session)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("can append version 1 then version 2")
  fun s11(tc: VertxTestContext) {
    val customer = Customer.create(id = 1, name = "c1")

    val cmd1 = CustomerCommand.ActivateCustomer("is needed")
    val metadata1 = CommandMetadata(1)
    val session1 = StatefulSession(0, customer.state, customerEventHandler)
    session1.execute { it.activate(cmd1.reason) }

    val cmd2 = CustomerCommand.DeactivateCustomer("it's not needed anymore")
    val metadata2 = CommandMetadata(1)
    val session2 = StatefulSession(1, customer.state, customerEventHandler)
    session2.execute { it.deactivate(cmd1.reason) }

    eventStore.append(cmd1, metadata1, session1)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        eventStore.append(cmd2, metadata2, session2)
          .onFailure { tc.failNow(it) }
          .onSuccess { tc.completeNow() }
      }
  }

  @Test
  @DisplayName("cannot append version 1 twice")
  fun s2(tc: VertxTestContext) {
    val customer = Customer.create(id = 1, name = "c1")

    val cmd1 = CustomerCommand.ActivateCustomer("is needed")
    val metadata1 = CommandMetadata(1)
    val session1 = StatefulSession(0, customer.state, customerEventHandler)
    session1.execute { it.activate(cmd1.reason) }

    val cmd2 = CustomerCommand.DeactivateCustomer("it's not needed anymore")
    val metadata2 = CommandMetadata(1)
    val session2 = StatefulSession(0, customer.state, customerEventHandler)
    session2.execute { it.deactivate(cmd1.reason) }

    eventStore.append(cmd1, metadata1, session1)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        eventStore.append(cmd2, metadata2, session2)
          .onSuccess { tc.failNow("should fail") }
          .onFailure { err ->
            // tc.verify { assertThat(err.message).isEqualTo("The current version is already the expected new version 1") }
            tc.completeNow()
          }
      }
  }

  @Test
  @DisplayName("cannot append version 3 after version 1")
  fun s22(tc: VertxTestContext) {
    val customer = Customer.create(id = 1, name = "c1")

    val cmd1 = CustomerCommand.ActivateCustomer("is needed")
    val metadata1 = CommandMetadata(1)
    val session1 = StatefulSession(0, customer.state, customerEventHandler)
    session1.execute { it.activate(cmd1.reason) }

    val cmd2 = CustomerCommand.DeactivateCustomer("it's not needed anymore")
    val metadata2 = CommandMetadata(1)
    val session2 = StatefulSession(2, customer.state, customerEventHandler)
    session2.execute { it.deactivate(cmd1.reason) }

    eventStore.append(cmd1, metadata1, session1)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        eventStore.append(cmd2, metadata2, session2)
          .onSuccess { tc.failNow("should fail") }
          .onFailure { err ->
//            tc.verify { assertThat(err.message).isEqualTo("The current version [1] should be [2]") }
            tc.completeNow()
          }
      }
  }
}

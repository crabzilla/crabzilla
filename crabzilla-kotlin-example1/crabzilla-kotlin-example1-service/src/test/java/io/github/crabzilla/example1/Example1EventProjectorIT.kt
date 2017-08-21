package io.github.crabzilla.example1

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Guice
import io.vertx.core.Vertx
import org.assertj.core.api.Assertions.assertThat
import io.github.crabzilla.example1.customer.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerCreated
import io.github.crabzilla.example1.customer.CustomerId
import io.github.crabzilla.model.DomainEvent
import io.github.crabzilla.stack.DbConcurrencyException
import io.github.crabzilla.stack.EventProjector
import io.github.crabzilla.stack.ProjectionData
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*
import java.util.Arrays.asList
import javax.inject.Inject

@DisplayName("An Example1EventProjector")
class Example1EventProjectorIT {

  @Inject
  internal lateinit var mapper: ObjectMapper

  @Inject
  internal lateinit var jdbi: Jdbi

  @Inject
  internal lateinit var eventProjector: EventProjector<CustomerSummaryDao>

  @BeforeEach
  fun setup() {

    Guice.createInjector(Example1Module(Vertx.vertx())).injectMembers(this)

    val h = jdbi.open()
    h.createScript("DELETE FROM units_of_work").execute()
    h.createScript("DELETE FROM customer_summary").execute()
    h.commit()

  }


  @Test
  @Throws(DbConcurrencyException::class)
  fun can_project_two_events() {

    val id = CustomerId("customer#1")
    val event1 = CustomerCreated(id, "customer1")
    val event2 = CustomerActivated("a good reason", Instant.now())
    val projectionData = ProjectionData(UUID.randomUUID(), 1L, id.stringValue(), asList<DomainEvent>(event1, event2))

    eventProjector!!.handle(listOf(projectionData))

    val h = jdbi.open()
    val dao = h.attach(CustomerSummaryDao::class.java)
    val fromDb = dao.getAll()[0]
    h.commit()
    //    System.out.printf("from  db: " + fromDb);

    assertThat(fromDb).isEqualToComparingFieldByField(CustomerSummary(id.stringValue(), event1.name, true))

  }

}

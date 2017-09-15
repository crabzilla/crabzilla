package io.github.crabzilla.example1;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import io.github.crabzilla.core.entity.EntityUnitOfWork;
import io.github.crabzilla.core.entity.Version;
import io.github.crabzilla.example1.customer.Customer;
import io.github.crabzilla.example1.customer.CustomerData;
import io.github.crabzilla.vertx.entity.EntityCommandExecution;
import io.vertx.core.json.Json;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static io.github.crabzilla.vertx.entity.EntityCommandExecution.RESULT.HANDLING_ERROR;
import static io.github.crabzilla.vertx.entity.EntityCommandExecution.RESULT.SUCCESS;
import static io.github.crabzilla.vertx.helpers.StringHelper.aggregateRootId;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class Example1AcceptanceIT {

  @BeforeClass
  public static void configureRestAssured() throws InterruptedException {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = Integer.getInteger("http.port");
    log.info("----> RestAssured.port=" + RestAssured.port);
  }

  @AfterClass
  public static void unconfigureRestAssured() {
    RestAssured.reset();
  }

  // tag::create_customer_test[]

  @Test
  public void successScenario() {

    val customerId = new CustomerData.CustomerId(UUID.randomUUID().toString());
    val createCustomerCmd = new CustomerData.CreateCustomer(UUID.randomUUID(), customerId, "customer test");
    val expectedEvent = new CustomerData.CustomerCreated(createCustomerCmd.getTargetId(), "customer test");
    val expectedUow = new EntityUnitOfWork(UUID.randomUUID(), createCustomerCmd,
            new Version(1), singletonList(expectedEvent));

    val json = Json.encodePrettily(createCustomerCmd);

    val response = given().
            contentType(JSON).
            body(json).
            when().
            put("/" + aggregateRootId(Customer.class) + "/commands").
            then().
            statusCode(201).
            contentType(ContentType.JSON)
            .extract().response().asString();

    val result = Json.decodeValue(response, EntityCommandExecution.class);

    assertThat(result.getResult()).isEqualTo(SUCCESS);
    assertThat(result.getCommandId()).isEqualTo(createCustomerCmd.getCommandId());
    assertThat(result.getConstraints().isEmpty());

    val uow = result.getUnitOfWork();

    assertThat(uow.targetId()).isEqualTo(expectedUow.targetId());
    assertThat(uow.getCommand()).isEqualTo(expectedUow.getCommand());
    assertThat(uow.getEvents()).isEqualTo(expectedUow.getEvents());
    assertThat(uow.getVersion()).isEqualTo(expectedUow.getVersion());

  }

  // end::create_customer_test[]

  @Test
  public void handlingErrorScenario() {

    val customerId = new CustomerData.CustomerId(UUID.randomUUID().toString());
    val activateCustomer = new CustomerData.ActivateCustomer(UUID.randomUUID(), customerId, "customer test");
    val json = Json.encodePrettily(activateCustomer);

    val response = given().
            contentType(JSON).
            body(json).
            when().
            put("/" + aggregateRootId(Customer.class) + "/commands").
            then().
            statusCode(400).
            contentType(ContentType.JSON)
            .extract().response().asString();

    val result = Json.decodeValue(response, EntityCommandExecution.class);

    assertThat(result.getResult()).isEqualTo(HANDLING_ERROR);
    assertThat(result.getCommandId()).isEqualTo(activateCustomer.getCommandId());
    assertThat(result.getConstraints().isEmpty());

    val uow = result.getUnitOfWork();

    assertThat(uow).isNull();

  }

}
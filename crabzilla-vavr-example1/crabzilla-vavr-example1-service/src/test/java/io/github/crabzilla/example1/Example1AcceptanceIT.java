package io.github.crabzilla.example1;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import io.github.crabzilla.core.entity.EntityUnitOfWork;
import io.github.crabzilla.core.entity.Version;
import io.github.crabzilla.example1.customer.Customer;
import io.vertx.core.json.Json;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static io.github.crabzilla.example1.customer.CustomerData.*;
import static io.github.crabzilla.vertx.helpers.StringHelper.aggregateId;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class Example1AcceptanceIT {

  static final String LOCATION_HEADER = "Location";

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
  public void createCustomer() {

    val customerId = new CustomerId(UUID.randomUUID().toString());
    val createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer test");
    val expectedEvent = new CustomerCreated(createCustomerCmd.getTargetId(), "customer test");
    val expectedUow = new EntityUnitOfWork(UUID.randomUUID(), createCustomerCmd,
            new Version(1), singletonList(expectedEvent));

    val json = Json.encodePrettily(createCustomerCmd);

    log.info("command=\n" + json);

    val postCmdResponse = given().
            contentType(JSON).
            body(json).
            when().
            post("/" + aggregateId(Customer.class) + "/commands").
            then().
            extract().response();

    assertThat(postCmdResponse.statusCode()).isEqualTo(201);
    assertThat(postCmdResponse.header(LOCATION_HEADER))
            .isEqualTo(RestAssured.baseURI + ":" + RestAssured.port + "/"
                    + aggregateId(Customer.class) + "/commands/" + createCustomerCmd.getCommandId().toString());

    val getUowResponse = given().
            contentType(JSON).
            body(json).
            when().
            get(postCmdResponse.header(LOCATION_HEADER)).
            then().
            statusCode(200).
            contentType(ContentType.JSON)
            .extract().response().asString();

    val uow = Json.decodeValue(getUowResponse, EntityUnitOfWork.class);

    assertThat(uow.targetId()).isEqualTo(expectedUow.targetId());
    assertThat(uow.getCommand()).isEqualTo(expectedUow.getCommand());
    assertThat(uow.getEvents()).isEqualTo(expectedUow.getEvents());
    assertThat(uow.getVersion()).isEqualTo(expectedUow.getVersion());

  }

  // end::create_customer_test[]


  @Test
  public void createCustomerIdempotency() {

    val customerId = new CustomerId(UUID.randomUUID().toString());
    val createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "customer test");
    val expectedEvent = new CustomerCreated(createCustomerCmd.getTargetId(), "customer test");
    val expectedUow = new EntityUnitOfWork(UUID.randomUUID(), createCustomerCmd,
            new Version(1), singletonList(expectedEvent));

    val json = Json.encodePrettily(createCustomerCmd);

    log.info("command=\n" + json);

    val postCmdResponse = given().
            contentType(JSON).
            body(json).
            when().
            post("/" + aggregateId(Customer.class) + "/commands").
            then().
            extract().response();

    assertThat(postCmdResponse.statusCode()).isEqualTo(201);
    assertThat(postCmdResponse.header(LOCATION_HEADER))
            .isEqualTo(RestAssured.baseURI + ":" + RestAssured.port + "/"
                    + aggregateId(Customer.class) + "/commands/" + createCustomerCmd.getCommandId().toString());

    val getUowResponse = given().
            contentType(JSON).
            body(json).
            when().
            get(postCmdResponse.header(LOCATION_HEADER)).
            then().
            statusCode(200).
            contentType(ContentType.JSON)
            .extract().response().asString();

    val uow = Json.decodeValue(getUowResponse, EntityUnitOfWork.class);

    assertThat(uow.targetId()).isEqualTo(expectedUow.targetId());
    assertThat(uow.getCommand()).isEqualTo(expectedUow.getCommand());
    assertThat(uow.getEvents()).isEqualTo(expectedUow.getEvents());
    assertThat(uow.getVersion()).isEqualTo(expectedUow.getVersion());

    // now lets post it again

    val postCmdResponse2 = given().
            contentType(JSON).
            body(json).
            when().
            post("/" + aggregateId(Customer.class) + "/commands").
            then().
            extract().response();

    assertThat(postCmdResponse2.statusCode()).isEqualTo(201);
    assertThat(postCmdResponse2.header(LOCATION_HEADER))
            .isEqualTo(RestAssured.baseURI + ":" + RestAssured.port + "/"
                    + aggregateId(Customer.class) + "/commands/" + createCustomerCmd.getCommandId().toString());

  }

  @Test
  public void unknownCustomer() {

    val customerId = new CustomerId(UUID.randomUUID().toString());
    val activateCustomer = new ActivateCustomer(UUID.randomUUID(), customerId, "customer test");
    val json = Json.encodePrettily(activateCustomer);

    log.info("command=\n" + json);

    val response = given().
            contentType(JSON).
            body(json).
            when().
            post("/" + aggregateId(Customer.class) + "/commands").
            then().
            statusCode(400).
            extract().response().asString();

    assertThat(response).isEmpty();

  }

}
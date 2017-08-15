package crabzilla.example1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import crabzilla.example1.aggregates.CustomerData;
import crabzilla.model.EntityCommand;
import crabzilla.model.EntityUnitOfWork;
import crabzilla.model.Version;
import io.vertx.core.json.Json;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class JacksonJsonTest {

  ObjectMapper mapper = Json.mapper;

  @BeforeEach
  public void setup() {
    mapper.registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());
   // mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
  }

  @Test
  public void one_event() throws Exception {

    val id = new CustomerData.CustomerId(UUID.randomUUID().toString());
    val command = new CustomerData.CreateCustomerCmd(UUID.randomUUID(), id, "customer1");
    val event = new CustomerData.CustomerCreated(id, command.getName());
    val uow1 = EntityUnitOfWork.unitOfWork(command, Version.create(1), Collections.singletonList(event));

    val uowAsJson = mapper.writeValueAsString(uow1);

    System.out.println(mapper.writerFor(EntityCommand.class).writeValueAsString(command));
    System.out.println(uowAsJson);

    val uow2 = mapper.readValue(uowAsJson, EntityUnitOfWork.class);

    assertThat(uow2).isEqualTo(uow1);

  }

  @Test
  public void more_events() throws Exception {

    val id = new CustomerData.CustomerId("customer#1");
    val command = new CustomerData.CreateCustomerCmd(UUID.randomUUID(), id, "customer1");
    val event1 = new CustomerData.CustomerCreated(id, command.getName());
    val event2 = new CustomerData.CustomerActivated("a rgood reason", Instant.now());

    val uow1 = EntityUnitOfWork.unitOfWork(command, Version.create(1), asList(event1,  event2));

    val uowAsJson = mapper.writeValueAsString(uow1);

    System.out.println(uowAsJson);

    val uow2 = mapper.readValue(uowAsJson, EntityUnitOfWork.class);

    assertThat(uow2).isEqualTo(uow1);

  }

}

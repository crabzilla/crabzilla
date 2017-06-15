import com.fasterxml.jackson.databind.ObjectMapper;
import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.events.CustomerActivated;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.model.Command;
import crabzilla.model.UnitOfWork;
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
    mapper.findAndRegisterModules();
    mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
  }

  @Test
  public void one_event() throws Exception {

    val id = new CustomerId(UUID.randomUUID().toString());
    val command = new CreateCustomerCmd(UUID.randomUUID(), id, "customer1");
    val event = new CustomerCreated(id, command.getName());
    val uow1 = UnitOfWork.unitOfWork(command, Version.create(1), Collections.singletonList(event));

    val uowAsJson = mapper.writeValueAsString(uow1);

    System.out.println(mapper.writerFor(Command.class).writeValueAsString(command));

    System.out.println(uowAsJson);

    val uow2 = mapper.readValue(uowAsJson, UnitOfWork.class);

    assertThat(uow2).isEqualTo(uow1);

  }

  @Test
  public void more_events() throws Exception {

    val id = new CustomerId("customer#1");
    val command = new CreateCustomerCmd(UUID.randomUUID(), id, "customer1");
    val event1 = new CustomerCreated(id, command.getName());
    val event2 = new CustomerActivated("a rgood reason", Instant.now());

    val uow1 = UnitOfWork.unitOfWork(command, Version.create(1), asList(event1,  event2));

    val uowAsJson = mapper.writeValueAsString(uow1);

    //System.out.println(uowAsJson);

    val uow2 = mapper.readValue(uowAsJson, UnitOfWork.class);

    assertThat(uow2).isEqualTo(uow1);

  }

}

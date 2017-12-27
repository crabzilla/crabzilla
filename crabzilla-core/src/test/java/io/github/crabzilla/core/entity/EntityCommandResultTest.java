//package io.github.crabzilla.core.entity;
//
//import io.github.crabzilla.core.DomainEvent;
//import lombok.val;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//
//import java.util.UUID;
//
//import static io.github.crabzilla.core.example1.customer.CustomerData.*;
//import static java.util.Arrays.asList;
//import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.junit.jupiter.api.Assertions.fail;
//
//@DisplayName("An EntityCommandResult")
//public class EntityCommandResultTest {
//
//  EntityCommandResult result;
//
//  final Version version = new Version(1);
//  final CustomerId customerId = new CustomerId("c1");
//  final UUID commandId = UUID.randomUUID();
//  final DomainEvent event = new CustomerCreated(customerId, "c1");
//  final EntityUnitOfWork uow = new EntityUnitOfWork(UUID.randomUUID(),
//          new CreateCustomer(commandId, customerId, "c1"),
//          version,
//          asList(event));
//
//  @Test
//  @DisplayName("Success can be instantiated")
//  public void successCanBeInstantiated() {
//    val result = EntityCommandResult.Companion.success(uow);
//    assertThat(result).isNotNull();
//  }
//
//  @Test
//  @DisplayName("Success can not be null")
//  public void successCannotBeNull() {
//    assertThrows(NullPointerException.class, () -> {
//      val result = EntityCommandResult.Companion.success(null);
//    });
//  }
//
//  @Test
//  @DisplayName("Error can be instantiated")
//  public void errorCanBeInstantiated() {
//    val result = EntityCommandResult.Companion.error(new RuntimeException("test"));
//    assertThat(result).isNotNull();
//  }
//
//  @Test
//  @DisplayName("Error cannot be null")
//  public void errorCannnotBeNull() {
//    assertThrows(NullPointerException.class, () -> {
//      val result = EntityCommandResult.Companion.error(null);
//    });
//  }
//
//  @Nested
//  @DisplayName("When is success")
//  public class WhenIsSuccess {
//
//    @BeforeEach
//    void setUp() {
//      result = EntityCommandResult.Companion.success(uow);
//    }
//
//    @Test
//    @DisplayName("success must run success block")
//    void successMustRunSuccessBlock() {
//      result.inCaseOfSuccess(uow -> assertThat(result).isNotNull());
//    }
//
//    @Test
//    @DisplayName("success cannot run an error block")
//    void successMustNotRunErrorBlock() {
//      result.inCaseOfError(uow -> fail("success cannot run an error block"));
//    }
//
//  }
//
//  @Nested
//  @DisplayName("When is error")
//  public class WhenIsError {
//
//    @BeforeEach
//    void setUp() {
//      result = EntityCommandResult.Companion.error(new RuntimeException("test"));
//    }
//
//    @Test
//    @DisplayName("error must run error block")
//    void errorMustRunErrorBlock() {
//      result.inCaseOfError(uow -> assertThat(result).isNotNull());
//    }
//
//    @Test
//    @DisplayName("error cannot run an success block")
//    void errorMustNotRunSuccessBlock() {
//      result.inCaseOfSuccess(uow -> fail("error cannot run an success block"));
//    }
//
//  }
//}
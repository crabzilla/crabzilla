package crabzilla.example1.aggregates.customer;

import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.services.SampleService;
import crabzilla.model.*;
import lombok.val;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

public class InvokedDynCommandHandlerFn<A extends AggregateRoot>
        implements BiFunction<Command, Snapshot<A>, Either<Exception, Optional<UnitOfWork>>> {

  final Object target;

  public InvokedDynCommandHandlerFn(Object target) {
    this.target = target;
  }

  public Either<Exception, Optional<UnitOfWork>> apply(final Command command, final Snapshot<A> snapshot) {

    MethodType methodType = MethodType.methodType(UnitOfWork.class, new Class<?>[] {command.getClass(), Snapshot.class});
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    try {
      MethodHandle methodHandle = lookup.findVirtual(target.getClass(), "handle", methodType);
      val uow = (UnitOfWork) methodHandle.invoke(command, snapshot);
      System.out.println("*** " + uow);
      return Eithers.right(Optional.ofNullable(uow)) ;
    } catch (NoSuchMethodException e) {
       return Eithers.right(Optional.empty());
    } catch (Throwable throwable) {
      throwable.printStackTrace();
      return Eithers.right(Optional.empty());
    }

//    try {
//      val unitOfWork = ((UnitOfWork) mm.invoke(this, command, snapshot));
//      return Eithers.right(Optional.ofNullable(unitOfWork));
//    } catch (IllegalAccessException | NoSuchMethodException e) {
//      return Eithers.right(Optional.empty());
//    } catch (Exception e) {
//      return Eithers.left(e);
//    }

  }

  public static void main(String[] args) {

    val service = new SampleService() {
      @Override
      public UUID uuid() {
        return UUID.randomUUID();
      }

      @Override
      public Instant now() {
        return Instant.now();
      }
    };

    val id = new InvokedDynCommandHandlerFn(new CustomerCmdHandlerFn(new CustomerStateTransitionFn(), c -> c.withService(service))) ;

    val customerId = new CustomerId(UUID.randomUUID().toString());
//    val customerId = new CustomerId("customer123");
    val createCustomerCmd = new CreateCustomerCmd(UUID.randomUUID(), customerId, "a good customer");

    id.apply(createCustomerCmd, new Snapshot<>(new CustomerSupplierFn().get(), new Version(0)));


  }

}

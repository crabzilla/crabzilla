package crabzilla.example1.aggregates.customer;

import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmdValidator;
import crabzilla.model.CommandValidatorFn;
import lombok.val;

import java.util.List;

import static java.util.Collections.emptyList;

public class CustomerCommandValidatorFn extends CommandValidatorFn {

    public List<String> validate(CreateCustomerCmd cmd) {

      val either = new CreateCustomerCmdValidator().validate(cmd).toEither();

      return either.isRight() ? emptyList() : either.getLeft().toJavaList();

    }

}

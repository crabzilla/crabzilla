package crabzilla.example1.aggregates.customer.commands;

import crabzilla.example1.aggregates.customer.CustomerId;
import javaslang.Function3;
import javaslang.collection.CharSeq;
import javaslang.collection.List;
import javaslang.control.Validation;

import java.util.UUID;

public class CreateCustomerCmdValidator {

    private static final String VALID_NAME_CHARS = "[a-zA-Z ]";

    public Validation<List<String>, CreateCustomerCmd> validate(CreateCustomerCmd cmd) {
      return Validation.combine(validateCmdId(cmd.getCommandId()),
                                validateId(cmd.getTargetId()),
                                validateName(cmd.getName())
      ).ap((Function3<UUID, CustomerId, String, CreateCustomerCmd>) CreateCustomerCmd::new);
    }

  private Validation<String, UUID>  validateCmdId(UUID commandId) {
    return commandId == null
            ? Validation.invalid("CommandId cannot be null ")
            : Validation.valid(commandId);
  }

  private Validation<String, CustomerId> validateId(CustomerId id) {
    return id == null
            ? Validation.invalid("CustomerId cannot be null ")
            : Validation.valid(id);
  }

  private Validation<String, String> validateName(String name) {
      return CharSeq.of(name).replaceAll(VALID_NAME_CHARS, "").transform(seq -> seq.isEmpty()
              ? Validation.valid(name)
              : Validation.invalid("Name contains invalid characters: '"
              + seq.distinct().sorted() + "'"));
    }

  }
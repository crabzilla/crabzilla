package crabzilla.stack;

import crabzilla.model.Command;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.singletonList;

public abstract class AbstractCommandValidatorFn implements Function<Command, List<String>> {

  static final String METHOD_NAME = "validate";
  final MethodHandles.Lookup lookup = MethodHandles.lookup();

  @SuppressWarnings(value = "unchecked")
  public List<String> apply(Command command) {

    if (command == null) {
      return singletonList("Command cannot be null.");
    }

    final MethodType methodType =
            MethodType.methodType(List.class, new Class<?>[] {command.getClass()});

    try {
      final MethodHandle methodHandle = lookup.bind(this, METHOD_NAME, methodType);
      return (List<String>) methodHandle.invokeWithArguments(command);
    } catch (Throwable e) {
      return singletonList(e.getMessage());
    }

  }

}

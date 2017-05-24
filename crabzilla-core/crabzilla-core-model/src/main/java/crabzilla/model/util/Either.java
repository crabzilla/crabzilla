package crabzilla.model.util;

import org.derive4j.Data;

import java.util.function.Function;

@Data
public interface Either<A, B> {
  <X> X match(Function<A, X> left, Function<B, X> right);
}

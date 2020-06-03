package io.github.crabzilla.hazelcast.example1;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.asList;

public class Test {

  public static void main(String[] args) {

    Vertx vertx = Vertx.vertx();

    // evento -> repositoy -> () -> Future<Void>

//    chainCall(asList("1","2", "3", "4"), str -> str.equals("3") ? Future.failedFuture("erro 3") : Future.succeededFuture("* " + str))
    chainCall(asList("1","2", "3", "4"), str -> Future.succeededFuture("* " + str))
    .onSuccess(System.out::println)
    .onFailure(Throwable::printStackTrace);

  }

  public static <T> Future<String> chainCall(List<T> list, Function<T, Future<String>> method){
    return list.stream().reduce(Future.succeededFuture(),// the initial "future"
      (acc, item) -> acc.compose(v -> method.apply(item)), // we return the compose of the previous "future" with "future" returned by next item processing
      (a,b) -> Future.succeededFuture()); // not used! only useful for parallel stream.
  }
}

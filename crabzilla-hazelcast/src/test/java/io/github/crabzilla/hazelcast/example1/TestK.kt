package io.github.crabzilla.hazelcast.example1

import io.vertx.core.Future
import io.vertx.core.Vertx

fun main(args: Array<String>) {

  val vertx = Vertx.vertx()

  // evento -> repositoy -> () -> Future<Void>
  chainCall(listOf("1", "2", "3", "4"))
      { str: String -> Future.succeededFuture("* $str") }
//      { str: String -> if (str == "3") Future.failedFuture("erro 3") else Future.succeededFuture("* $str") }
    .onSuccess { result: String -> println(result) }
    .onFailure { err: Throwable -> err.printStackTrace() }
}

fun <T> chainCall(list: List<T>, method: (T) -> Future<String>): Future<String> {
  return list.stream().reduce(Future.succeededFuture(), // the initial "future"
    { previousFuture: Future<String>, item: T -> previousFuture.compose { method.invoke(item) } }, // we return the compose of the previous "future" with "future" returned by next item processing
    { _, _ -> Future.succeededFuture() }) // not used! only useful for parallel stream.
}

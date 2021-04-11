package io.github.crabzilla

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import java.util.function.BiFunction

object FoldFuturesTest {
  @JvmStatic
  fun main(args: Array<String>) {
    list()
  }

  private fun list() {
    val l: MutableList<String> = ArrayList()
    l.add("one")
    l.add("two")
    l.add("three")
    val f = Future.succeededFuture<Void>()
    foldLeft(
      l.iterator(), f,
      { f1: Future<Void?>, s: String -> f1.compose { ignore: Void? -> action(s) } }
    )
      .onComplete { res: AsyncResult<Void?>? -> println("completed") }
  }

  private fun action(s: String): Future<Void?> {
    println("processing $s")
    if ("two".equals(s)) {
      return Future.failedFuture("two is bad")
    }
    return Future.succeededFuture()
  }

  private fun <A, B> foldLeft(iterator: Iterator<A>, identity: B, bf: BiFunction<B, A, B>): B {
    var result = identity
    while (iterator.hasNext()) {
      val next = iterator.next()
      result = bf.apply(result, next)
    }
    return result
  }
}

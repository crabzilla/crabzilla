package io.github.crabzilla.pgc

import io.vertx.core.Future
import io.vertx.core.Promise
import org.slf4j.LoggerFactory

fun main() {

  val log = LoggerFactory.getLogger("test")

  fun generate(size: Int): List<Future<Void>> {
    return MutableList<Future<Void>>(size) { Future.succeededFuture() }
  }

  fun <T> compose(futures: List<Future<T>>): Future<T> {
    val promise = Promise.promise<T>()
    val ok = Future.succeededFuture<T>()
    futures[0]
      .compose { if (futures.size > 1) futures[1] else ok }
      .compose { if (futures.size > 2) futures[2] else ok }
      .compose { if (futures.size > 3) futures[3] else ok }
      .compose { if (futures.size > 4) futures[4] else ok }
      .compose { if (futures.size > 5) futures[5] else ok }
      .compose { if (futures.size > 6) futures[6] else ok }
      .compose { if (futures.size > 7) futures[7] else ok }
      .compose { if (futures.size > 8) futures[8] else ok }
      .compose { if (futures.size > 9) futures[9] else ok }
      .compose { if (futures.size > 10) futures[10] else ok }
      .compose { if (futures.size > 11) futures[11] else ok }
      .compose { if (futures.size > 12) futures[12] else ok }
      .compose { if (futures.size > 13) futures[13] else ok }
      .compose { if (futures.size > 14) futures[14] else ok }
      .compose { if (futures.size > 15) futures[15] else ok }
      .compose { if (futures.size > 16) futures[16] else ok }
      .compose { if (futures.size > 17) futures[17] else ok }
      .compose { if (futures.size > 18) futures[18] else ok }
      .onFailure { promise.fail(it) }
      .onSuccess { promise.complete(it) }
    return promise.future()
  }

  fun <T> compose2(futures: List<Future<T>>): Future<T> {
    val promise = Promise.promise<T>()
    fun complete(i: Int, result: T) {
      log.warn("Will stop at step $i")
      promise.complete(result)
    }
    fun fail(i: Int, error: Throwable) {
      log.warn("Failed at step $i")
      promise.fail(error)
    }
    futures[0]
      .onFailure { fail(0, it) }
      .onSuccess {
        if (futures.size == 1) complete(1, it) else futures[1]
          .onFailure { fail(1, it) }
          .onSuccess {
            if (futures.size == 2) complete(2, it) else futures[2]
              .onFailure { fail(2, it) }
              .onSuccess {
                if (futures.size == 3) complete(3, it) else futures[3]
                  .onFailure { fail(3, it) }
                  .onSuccess {
                    if (futures.size == 4) complete(4, it) else futures[4]
                      .onFailure { fail(4, it) }
                      .onSuccess {
                        if (futures.size == 5) complete(5, it) else futures[5]
                          .onFailure { fail(5, it) }
                          .onSuccess {
                            if (futures.size == 6) complete(6, it) else futures[6]
                              .onFailure { fail(6, it) }
                              .onSuccess {
                                if (futures.size == 7) complete(7, it) else futures[7]
                                  .onFailure { fail(7, it) }
                                  .onSuccess {
                                    if (futures.size == 8) complete(8, it) else futures[8]
                                      .onFailure { fail(9, it) }
                                      .onSuccess { complete(9, it) }
                                  }
                              }
                          }
                      }
                  }
              }
          }
      }
    return promise.future()
  }

  val futures = generate(3)
  val list = listOf<Future<Void>>(Future.failedFuture("Erro no 0")).plus(futures)

  compose2(list)
    .onSuccess { log.info("god") }
    .onFailure { log.error("bad", it) }
}

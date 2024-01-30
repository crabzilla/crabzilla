package io.github.crabzilla

import io.vertx.core.json.JsonObject
import io.vertx.core.json.pointer.JsonPointer
import org.junit.jupiter.api.Test

class TestJson {
  @Test
  fun t1() {
    val j1 = JsonObject().put("a", 1).put("b", 3).put("c", 33)
    val j2 = JsonObject().put("b", 2).put("child", j1)
    val j3 = JsonObject().put("c", j2)

    println(j3.encodePrettily())

    val jp = JsonPointer.from("/c/b/child/c")

    val x = jp.queryJson(j3)

    println(x)
  }
}

package io.github.crabzilla

import java.lang.Integer.max
import java.util.*

fun main() {
  println(e2(arrayOf(0, 0, 0), 1).joinToString())
  println(e2(arrayOf(3, 8, 9, 7, 6), 3).joinToString())
}

fun e2(array: Array<Int>, k: Int): IntArray {
  val list = array.toList()
  Collections.rotate(list, k)
  return list.toIntArray()
}

fun e1(num: Int): Int {
  val b = num.toString(2)
  println(b)
  var currentBiggest = 0
  var biggest = 0
  b.forEach {
    if (it == '1') {
      biggest = max(currentBiggest, biggest)
      currentBiggest = 0
    } else {
      currentBiggest += 1
    }
  }
  return biggest
}

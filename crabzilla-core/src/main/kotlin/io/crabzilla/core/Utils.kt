package io.crabzilla.core

fun <S, C> buildException(
  state: S,
  command: C,
): IllegalStateException {
  return IllegalStateException(
    "Illegal transition. " +
      "state: ${state!!::class.java.simpleName} command: ${command!!::class.java.simpleName}",
  )
}

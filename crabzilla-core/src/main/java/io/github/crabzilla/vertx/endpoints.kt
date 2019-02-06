package io.github.crabzilla.vertx

import java.util.regex.Pattern

internal const val COMMAND_HANDLER = "-cmd-handler"
internal const val EVENTS_HANDLER = "-events-handler"

fun restEndpoint(name: String): String {
  return camelCaseToSpinalCase(name)
}

fun cmdHandlerEndpoint(name: String): String {
  return camelCaseToSpinalCase(name) + COMMAND_HANDLER
}

fun projectorEndpoint(bcName: String): String {
  return camelCaseToSpinalCase(bcName) + EVENTS_HANDLER
}

private fun camelCaseToSpinalCase(start: String): String {
  val m = Pattern.compile("(?<=[a-z])[A-Z]").matcher(start)
  val sb = StringBuffer()
  while (m.find()) {
    m.appendReplacement(sb, "-" + m.group())
  }
  m.appendTail(sb)
  return sb.toString().toLowerCase()
}


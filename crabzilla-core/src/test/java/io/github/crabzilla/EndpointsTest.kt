package io.github.crabzilla

import org.junit.jupiter.api.Test

import org.assertj.core.api.Assertions.assertThat

internal class EndpointsTest {

  @Test
  fun restEndpoint() {
    assertThat("camel-case-endpoint").isEqualTo(restEndpoint("CamelCaseEndpoint"))
  }

  @Test
  fun cmdHandlerEndpoint() {
    assertThat("camel-case-endpoint-cmd-handler").isEqualTo(cmdHandlerEndpoint("CamelCaseEndpoint"))
  }

  @Test
  fun projectorEndpoint() {
    assertThat("camel-case-endpoint-events-handler").isEqualTo(projectorEndpoint("CamelCaseEndpoint"))
  }
}

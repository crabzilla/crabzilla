package io.github.crabzilla;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EndpointsTest {

  @Test
  void restEndpoint() {
    assertThat("camel-case-endpoint").isEqualTo(CrabzillaKt.restEndpoint("CamelCaseEndpoint"));
  }

  @Test
  void cmdHandlerEndpoint() {
    assertThat("camel-case-endpoint-cmd-handler").isEqualTo(CrabzillaKt.cmdHandlerEndpoint("CamelCaseEndpoint"));
  }

  @Test
  void projectorEndpoint() {
    assertThat("camel-case-endpoint-events-handler").isEqualTo(CrabzillaKt.projectorEndpoint("CamelCaseEndpoint"));
  }
}

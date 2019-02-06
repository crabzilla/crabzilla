package io.github.crabzilla.vertx;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EndpointsTest {

  @Test
  void restEndpoint() {
    assertThat("camel-case-endpoint").isEqualTo(EndpointsKt.restEndpoint("CamelCaseEndpoint"));
  }

  @Test
  void cmdHandlerEndpoint() {
    assertThat("camel-case-endpoint-cmd-handler").isEqualTo(EndpointsKt.cmdHandlerEndpoint("CamelCaseEndpoint"));
  }

  @Test
  void projectorEndpoint() {
    assertThat("camel-case-endpoint-events-handler").isEqualTo(EndpointsKt.projectorEndpoint("CamelCaseEndpoint"));
  }
}

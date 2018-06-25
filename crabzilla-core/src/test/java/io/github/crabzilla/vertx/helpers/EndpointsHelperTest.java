package io.github.crabzilla.vertx.helpers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EndpointsHelperTest {

  @Test
  void restEndpoint() {
    assertThat("camel-case-endpoint").isEqualTo(EndpointsHelper.restEndpoint("CamelCaseEndpoint"));
  }

  @Test
  void cmdHandlerEndpoint() {
    assertThat("camel-case-endpoint-cmd-handler").isEqualTo(EndpointsHelper.cmdHandlerEndpoint("CamelCaseEndpoint"));
  }

  @Test
  void projectorEndpoint() {
    assertThat("camel-case-endpoint-events-handler").isEqualTo(EndpointsHelper.projectorEndpoint("CamelCaseEndpoint"));
  }
}

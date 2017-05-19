package crabzilla.stack1.routes;

import org.apache.camel.ContextTestSupport;
import org.apache.camel.builder.RouteBuilder;

/**
 *
 */
public class SameSedaQueueMultipleConsumersResult extends ContextTestSupport {

  public void testSameOptionsProducerStillOkay() throws Exception {
    getMockEndpoint("mock:final-result").expectedBodiesReceivedInAnyOrder("hello-from-foo", "hello-from-bar");

    template.sendBody("seda:my-queue", "Hello World");

    assertMockEndpointsSatisfied();
  }

  @Override
  protected RouteBuilder createRouteBuilder() throws Exception {
    return new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        from("seda:my-queue?multipleConsumers=true").routeId("foo").to("direct:foo");
        from("seda:my-queue?multipleConsumers=true").routeId("bar").to("direct:bar");
        from("direct:foo").setBody(constant("hello-from-foo")).to("mock:final-result");
        from("direct:bar").setBody(constant("hello-from-bar")).to("mock:final-result");
      }
    };
  }
}
package crabzilla.stack1.routes;

import crabzilla.UnitOfWork;
import crabzilla.model.AggregateRoot;
import lombok.NonNull;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;

import java.util.List;

import static crabzilla.stack1.routes.HeadersConstants.*;
import static crabzilla.stack1.routes.StringHelper.*;

public class CommandRestRoute<A extends AggregateRoot> extends RouteBuilder {

	final Class<A> aggregateRootClass;
	final List<Class<?>> commandsClasses;

  public CommandRestRoute(@NonNull Class<A> aggregateRootClass, @NonNull List<Class<?>> commandsClasses) {
    this.aggregateRootClass = aggregateRootClass;
    this.commandsClasses = commandsClasses;
  }

  @Override
  public void configure() throws Exception {

    restConfiguration().component("undertow").bindingMode(RestBindingMode.auto)
            .dataFormatProperty("prettyPrint", "true")
            .contextPath("/").port(8080)
            .apiContextPath("/api-doc")
            .apiProperty("api.title", "Customer API").apiProperty("api.version", "1.0.0")
            .enableCORS(true);

    commandsClasses.forEach(this::createRouteForCommand);

  }

  private void createRouteForCommand(Class<?> commandClazz) {

    rest("/" + aggregateRootId(aggregateRootClass))
      .put("{" + AGGREGATE_ROOT_ID + "}/" + commandId(commandClazz) + "/{" + COMMAND_ID + "}")
            .id(aggrCmdRoot("put-", aggregateRootClass, commandClazz))
            .description("post a new " + commandId(commandClazz))
      .consumes(APPLICATION_JSON).type(commandClazz)
        .param()
          .name(AGGREGATE_ROOT_ID).description("the id of the target AggregateRoot instance")
          .type(RestParamType.query).dataType("java.util.String")
        .endParam()
        .param()
          .name(COMMAND_ID).description("the id of the requested functions")
          .type(RestParamType.query).dataType("java.util.String")
        .endParam()
      .produces(APPLICATION_JSON)
        .responseMessage()
          .code(201).responseModel(UnitOfWork.class).message("created")
        .endResponseMessage()
        .responseMessage()
          .code(400).responseModel(List.class).message("bad request")
        .endResponseMessage()
        .responseMessage()
          .code(503).responseModel(List.class).message("service unavailable")
        .endResponseMessage()
        .to("direct:handle-" + aggregateRootId(aggregateRootClass));

  }

}

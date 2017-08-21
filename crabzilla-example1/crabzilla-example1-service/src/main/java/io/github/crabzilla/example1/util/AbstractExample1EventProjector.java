package io.github.crabzilla.example1.util;

import io.github.crabzilla.example1.CustomerSummaryDao;
import io.github.crabzilla.model.DomainEvent;
import io.github.crabzilla.stack.EventProjector;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@Slf4j
public abstract class AbstractExample1EventProjector<DAO> extends EventProjector<DAO> {

  static final String METHOD_NAME = "handle";
  final MethodHandles.Lookup lookup = MethodHandles.lookup();

  protected AbstractExample1EventProjector(String eventsChannelId, Class<DAO> daoClass, Jdbi jdbi) {
    super(eventsChannelId, daoClass, jdbi);
  }

  public void write(CustomerSummaryDao dao, String targetId, DomainEvent event) {

    final MethodType methodType =
            MethodType.methodType(void.class, new Class<?>[] {daoClass, String.class, event.getClass()});

    try {
      final MethodHandle methodHandle = lookup.bind(this, METHOD_NAME, methodType);
      methodHandle.invokeWithArguments(dao, targetId, event);
    }
    catch (Throwable throwable) {
      log.error("When projecting events",  throwable);
    }

  }

}

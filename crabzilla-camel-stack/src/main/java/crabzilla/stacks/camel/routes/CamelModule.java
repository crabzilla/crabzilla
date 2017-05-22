//package crabzilla.stacks.camel.routes;
//
//import com.google.inject.AbstractModule;
//import com.google.inject.Provides;
//import com.google.inject.multibindings.OptionalBinder;
//import org.apache.camel.ProducerTemplate;
//import org.apache.camel.ThreadPoolRejectedPolicy;
//import org.apache.camel.impl.DefaultCamelContext;
//import org.apache.camel.impl.DefaultThreadPoolFactory;
//import org.apache.camel.impl.SimpleRegistry;
//import org.apache.camel.management.DefaultManagementLifecycleStrategy;
//import org.apache.camel.processor.interceptor.Tracer;
//import org.apache.camel.spi.ThreadPoolFactory;
//import org.apache.camel.spi.ThreadPoolProfile;
//
//import javax.inject.Named;
//import javax.inject.Singleton;
//import java.util.Optional;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
//public class CamelModule extends AbstractModule {
//
//    @Override
//    protected void configure() {
//      OptionalBinder.newOptionalBinder(binder(), SimpleRegistry.class);
//      OptionalBinder.newOptionalBinder(binder(), DefaultManagementLifecycleStrategy.class);
//    }
//
//    @Provides
//    @Singleton
//		DefaultCamelContext context(Optional<SimpleRegistry> registry,
//                                Optional<DefaultManagementLifecycleStrategy> lifecycleStrategy,
//                                @Named("camel_ctx_name") String camel_ctx_name,
//                                @Named("camel_tracer_enabled") boolean camel_tracer_enabled) throws Exception {
//
//        DefaultCamelContext context = registry.isPresent() ? new DefaultCamelContext(registry.get()) :
//                                                             new DefaultCamelContext();
//
//        context.setManagementName(camel_ctx_name);
//        context.setName(camel_ctx_name);
//
//        if (lifecycleStrategy.isPresent()) {
//          context.addLifecycleStrategy(lifecycleStrategy.get());
//        }
//
//        ThreadPoolProfile poolProfile = new ThreadPoolProfile("masterPoolProfile");
//        poolProfile.setPoolSize(2);
//        poolProfile.setMaxPoolSize(10);
//        poolProfile.setMaxQueueSize(100);
//        poolProfile.setKeepAliveTime(30L);
//        poolProfile.setTimeUnit(TimeUnit.MINUTES);
//        poolProfile.setRejectedPolicy(ThreadPoolRejectedPolicy.Abort);
//
//        ThreadPoolFactory poolFactory = new DefaultThreadPoolFactory();
//        poolFactory.newThreadPool(poolProfile, Executors.defaultThreadFactory());
//
//        context.getExecutorServiceManager().setThreadPoolFactory(poolFactory);
//
//        context.setTracing(camel_tracer_enabled);
//
//        if (camel_tracer_enabled) {
//            Tracer tracer = new Tracer();
//            tracer.getDefaultTraceFormatter().setShowBreadCrumb(false);
//            tracer.getDefaultTraceFormatter().setShowNode(false);
//            context.addInterceptStrategy(tracer);
//            // and only trace if the body contains London as text
//            // tracer.setTraceFilter(body().contains(constant("London")));
//        }
//
//        return context;
//    }
//
//    @Provides
//    @Singleton
//		ProducerTemplate producerTemplate(DefaultCamelContext context) {
//        return context.createProducerTemplate();
//    }
//
//}

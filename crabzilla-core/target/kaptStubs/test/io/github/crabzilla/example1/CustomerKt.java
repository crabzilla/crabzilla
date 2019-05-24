package io.github.crabzilla.example1;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 2, d1 = {"\u0000J\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\"Q\u0010\u0000\u001aB\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u0003\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\u0004\u0012\u0010\u0012\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00070\u0006\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\t0\u0001j\b\u0012\u0004\u0012\u00020\u0005`\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\f\"#\u0010\r\u001a\u0014\u0012\u0004\u0012\u00020\u0003\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100\u000f0\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012\"\u0011\u0010\u0013\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015\"#\u0010\u0016\u001a\u0014\u0012\u0004\u0012\u00020\u0018\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00050\u0017\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u001a\u00a8\u0006\u001b"}, d2 = {"CUSTOMER_CMD_HANDLER_FACTORY", "Lkotlin/Function4;", "Lio/github/crabzilla/CommandMetadata;", "Lio/github/crabzilla/Command;", "Lio/github/crabzilla/Snapshot;", "Lio/github/crabzilla/example1/Customer;", "Lio/vertx/core/Handler;", "Lio/vertx/core/AsyncResult;", "Lio/github/crabzilla/UnitOfWork;", "Lio/github/crabzilla/CommandHandler;", "Lio/github/crabzilla/CommandHandlerFactory;", "getCUSTOMER_CMD_HANDLER_FACTORY", "()Lkotlin/jvm/functions/Function4;", "CUSTOMER_CMD_VALIDATOR", "Lkotlin/Function1;", "", "", "getCUSTOMER_CMD_VALIDATOR", "()Lkotlin/jvm/functions/Function1;", "CUSTOMER_SEED_VALUE", "getCUSTOMER_SEED_VALUE", "()Lio/github/crabzilla/example1/Customer;", "CUSTOMER_STATE_BUILDER", "Lkotlin/Function2;", "Lio/github/crabzilla/DomainEvent;", "getCUSTOMER_STATE_BUILDER", "()Lkotlin/jvm/functions/Function2;", "crabzilla-core"})
public final class CustomerKt {
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.example1.Customer CUSTOMER_SEED_VALUE = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.jvm.functions.Function2<io.github.crabzilla.DomainEvent, io.github.crabzilla.example1.Customer, io.github.crabzilla.example1.Customer> CUSTOMER_STATE_BUILDER = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.jvm.functions.Function1<io.github.crabzilla.Command, java.util.List<java.lang.String>> CUSTOMER_CMD_VALIDATOR = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.jvm.functions.Function4<io.github.crabzilla.CommandMetadata, io.github.crabzilla.Command, io.github.crabzilla.Snapshot<io.github.crabzilla.example1.Customer>, io.vertx.core.Handler<io.vertx.core.AsyncResult<io.github.crabzilla.UnitOfWork>>, io.github.crabzilla.CommandHandler<io.github.crabzilla.example1.Customer>> CUSTOMER_CMD_HANDLER_FACTORY = null;
    
    @org.jetbrains.annotations.NotNull()
    public static final io.github.crabzilla.example1.Customer getCUSTOMER_SEED_VALUE() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public static final kotlin.jvm.functions.Function2<io.github.crabzilla.DomainEvent, io.github.crabzilla.example1.Customer, io.github.crabzilla.example1.Customer> getCUSTOMER_STATE_BUILDER() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public static final kotlin.jvm.functions.Function1<io.github.crabzilla.Command, java.util.List<java.lang.String>> getCUSTOMER_CMD_VALIDATOR() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public static final kotlin.jvm.functions.Function4<io.github.crabzilla.CommandMetadata, io.github.crabzilla.Command, io.github.crabzilla.Snapshot<io.github.crabzilla.example1.Customer>, io.vertx.core.Handler<io.vertx.core.AsyncResult<io.github.crabzilla.UnitOfWork>>, io.github.crabzilla.CommandHandler<io.github.crabzilla.example1.Customer>> getCUSTOMER_CMD_HANDLER_FACTORY() {
        return null;
    }
}

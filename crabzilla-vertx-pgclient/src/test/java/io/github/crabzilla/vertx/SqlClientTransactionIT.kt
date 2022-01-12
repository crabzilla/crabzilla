// package io.github.crabzilla.vertx
//
// import io.github.crabzilla.pgclient.command.CommandsContext
// import io.github.crabzilla.pgclient.command.connectOptions
// import io.github.crabzilla.pgclient.command.poolOptions
// import io.github.crabzilla.core.json.JsonSerDer
// import io.github.crabzilla.example1.example1Json
// import io.github.crabzilla.json.KotlinJsonSerDer
// import io.vertx.core.Vertx
// import io.vertx.junit5.VertxExtension
// import io.vertx.junit5.VertxTestContext
// import io.vertx.sqlclient.Tuple
// import org.junit.jupiter.api.Test
// import org.junit.jupiter.api.TestInstance
// import org.junit.jupiter.api.extension.ExtendWith
//
// @ExtendWith(VertxExtension::class)
// @TestInstance(TestInstance.Lifecycle.PER_CLASS)
// class SqlClientTransactionIT {
//
//  private lateinit var jsonSerDer: JsonSerDer
//  private lateinit var commandsContext: CommandsContext
//
//  @Test
//  fun shouldRollback(tc: VertxTestContext, vertx: Vertx) {
//    jsonSerDer = KotlinJsonSerDer(example1Json)
//    commandsContext = CommandsContext.create(vertx, jsonSerDer, connectOptions, poolOptions)
//    val theSqlClient = commandsContext.sqlClient
//    theSqlClient
//      .preparedQuery("begin")
//      .execute()
//      .compose {
//        theSqlClient.preparedQuery("insert into projections (name) values (null) returning name").execute()
//      }.onFailure {
//        theSqlClient.preparedQuery("rollback").execute().onFailure {
//          tc.failNow(it)
//        }.onSuccess {
//          tc.completeNow()
//          println("rollback")
//        }
//      }.onSuccess {
//        theSqlClient.preparedQuery("commit").execute().onFailure {
//          tc.failNow(it)
//        }.onSuccess {
//          tc.completeNow()
//          println("commit")
//        }
//      }
//  }
//
//  @Test
//  fun shouldCommit(tc: VertxTestContext, vertx: Vertx) {
//    jsonSerDer = KotlinJsonSerDer(example1Json)
//    commandsContext = CommandsContext.create(vertx, jsonSerDer, connectOptions, poolOptions)
//    val theSqlClient = commandsContext.sqlClient
//    theSqlClient
//      .preparedQuery("delete from projections where name = $1")
//      .execute(Tuple.of("xxx"))
//      .compose {
//        theSqlClient.preparedQuery("begin")
//          .execute()
//          .compose {
//            theSqlClient.preparedQuery("insert into projections (name) values ('xxx') returning name").execute()
//          }.onFailure {
//            theSqlClient.preparedQuery("rollback").execute().onFailure {
//              tc.failNow(it)
//            }.onSuccess {
//              tc.failNow("rollback error")
//              println("rollback")
//            }
//          }.onSuccess {
//            theSqlClient.preparedQuery("commit").execute().onFailure {
//              tc.failNow(it)
//            }.onSuccess {
//              tc.completeNow()
//              println("commit")
//            }
//          }
//      }
//  }
// }

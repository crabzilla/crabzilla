package io.github.crabzilla.pgc.query

import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class PgcProjectionRepoIT {

  // TODO
  @Disabled
  @Test
  fun `selectLastUowId works `(tc: VertxTestContext) {
//     writeDb.preparedQuery(PgcUowJournal.SQL_APPEND_UOW)
//       .execute(tuple1) { event1 ->
//         if (event1.failed()) {
//           event1.cause().printStackTrace()
//           tc.failNow(event1.cause())
//           return@execute
//         }
//         val uowId = event1.result().first().getLong(0)
//         tc.verify { tc.verify { Assertions.assertThat(uowId).isGreaterThan(0) } }
//         repo.selectLastUowId()
//           .onFailure { err -> tc.failNow(err) }
//           .onSuccess { result ->
//             tc.verify {
//               Assertions.assertThat(result).isEqualTo(uowId)
//               tc.completeNow()
//             }
//           }
//       }
  }
}

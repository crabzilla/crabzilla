package io.github.crabzilla.context

import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@DisplayName("Instantiating CrabzillaContext")
@ExtendWith(VertxExtension::class)
class CrabzillaContextTest : AbstractContextTest() {
  @Test
  fun `it can instantiate context`(vertx: Vertx) {
    val context = CrabzillaContextImpl(vertx, dbConfig)
    assertThat(context).isNotNull()
  }

  @Test
  fun `it can create a subscriber`(vertx: Vertx) {
    val context = CrabzillaContextImpl(vertx, dbConfig)
    assertThat(context.newPgSubscriber()).isNotNull
  }
}

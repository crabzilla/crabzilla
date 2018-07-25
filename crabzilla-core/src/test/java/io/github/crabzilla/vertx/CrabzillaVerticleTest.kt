package io.github.crabzilla.vertx

import io.vertx.core.Vertx
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CrabzillaVerticleTest {

  lateinit var vertx: Vertx

  @BeforeEach
  fun setUp() {

    vertx = Vertx.vertx()

    initVertx(vertx)

    //vertx.deployVerticle(verticle, tc.succeeding(x -> tc.completeNow()));

  }

  @Test
  fun crabzillaVerticleRole() {
    val v1 = SampleCrabzillaVerticle("test1", VerticleRole.HANDLER)
    assertThat("handler").isEqualTo(v1.role.prefix())
    assertThat("handler:test1").isEqualTo(v1.role.verticle("test1"))
  }

  @Test
  fun crabzillaVerticle() {
    val v1 = SampleCrabzillaVerticle("test1", VerticleRole.HANDLER)
    assertThat("test1").isEqualTo(v1.name)
    assertThat(VerticleRole.HANDLER).isEqualTo(v1.role)
  }

  @Test
  fun crabzillaVerticleFactory() {
    val v1 = SampleCrabzillaVerticle("test1", VerticleRole.HANDLER)
    val vf = CrabzillaVerticleFactory(setOf(v1), VerticleRole.HANDLER)
    assertThat("handler").isEqualTo(vf.prefix())
    assertThat(v1).isSameAs(vf.createVerticle("test1", this.javaClass.classLoader))
  }

  class SampleCrabzillaVerticle(override val name: String, override val role: VerticleRole) :
    CrabzillaVerticle(name, role)

}

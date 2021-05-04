// package io.github.crabzilla.pgc
//
// import io.vertx.core.Vertx
// import io.vertx.core.buffer.Buffer
// import io.vertx.core.net.PemTrustOptions
// import io.vertx.junit5.VertxExtension
// import io.vertx.junit5.VertxTestContext
// import io.vertx.pgclient.PgConnectOptions
// import io.vertx.pgclient.PgPool
// import io.vertx.pgclient.SslMode
// import io.vertx.sqlclient.PoolOptions
// import org.junit.jupiter.api.BeforeEach
// import org.junit.jupiter.api.DisplayName
// import org.junit.jupiter.api.Test
// import org.junit.jupiter.api.extension.ExtendWith
//
// @ExtendWith(VertxExtension::class)
// class CoachIT {
//
//  val pem = """
// -----BEGIN CERTIFICATE-----
// MIICUTCCAfugAwIBAgIBADANBgkqhkiG9w0BAQQFADBXMQswCQYDVQQGEwJDTjEL
// MAkGA1UECBMCUE4xCzAJBgNVBAcTAkNOMQswCQYDVQQKEwJPTjELMAkGA1UECxMC
// VU4xFDASBgNVBAMTC0hlcm9uZyBZYW5nMB4XDTA1MDcxNTIxMTk0N1oXDTA1MDgx
// NDIxMTk0N1owVzELMAkGA1UEBhMCQ04xCzAJBgNVBAgTAlBOMQswCQYDVQQHEwJD
// TjELMAkGA1UEChMCT04xCzAJBgNVBAsTAlVOMRQwEgYDVQQDEwtIZXJvbmcgWWFu
// ZzBcMA0GCSqGSIb3DQEBAQUAA0sAMEgCQQCp5hnG7ogBhtlynpOS21cBewKE/B7j
// V14qeyslnr26xZUsSVko36ZnhiaO/zbMOoRcKK9vEcgMtcLFuQTWDl3RAgMBAAGj
// gbEwga4wHQYDVR0OBBYEFFXI70krXeQDxZgbaCQoR4jUDncEMH8GA1UdIwR4MHaA
// FFXI70krXeQDxZgbaCQoR4jUDncEoVukWTBXMQswCQYDVQQGEwJDTjELMAkGA1UE
// CBMCUE4xCzAJBgNVBAcTAkNOMQswCQYDVQQKEwJPTjELMAkGA1UECxMCVU4xFDAS
// BgNVBAMTC0hlcm9uZyBZYW5nggEAMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEE
// BQADQQA/ugzBrjjK9jcWnDVfGHlk3icNRq0oV7Ri32z/+HQX67aRfgZu7KWdI+Ju
// Wm7DCfrPNGVwFWUQOmsPue9rZBgO
// -----END CERTIFICATE-----
//  """.trimIndent()
//
//  val connectOptions = PgConnectOptions()
//  .setPort(26257)
// //    .setPort(38375)
//  .setHost("127.0.0.1")
//  .setDatabase("movr")
//  .setUser("root")
//  .setPassword("admin")
//    .setSslMode(SslMode.DISABLE)
// //    .setPemTrustOptions( PemTrustOptions().addCertValue(Buffer.buffer(pem)));
//
//  val poolOptions = PoolOptions().setMaxSize(5);
//
//  lateinit var pgPool: PgPool
//
//  @BeforeEach
//  fun setup(vertx: Vertx, tc: VertxTestContext) {
//    pgPool = PgPool.pool(vertx, connectOptions, poolOptions)
//
//    println("Init")
//    tc.completeNow()
//  }
//
//  @Test
//  @DisplayName("cock")
//  fun a0(tc: VertxTestContext, vertx: Vertx) {
//
//    println("Test")
//    pgPool.query("show tables;").execute()
//      .onSuccess { rs ->
//        rs.forEach { r ->
//          println(r.toJson())
//        }
//        tc.completeNow()
//      }.onFailure {
//        tc.failNow(it)
//      }
//
//  }
//
// }

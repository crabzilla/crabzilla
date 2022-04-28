package io.github.crabzilla

import io.github.crabzilla.example1.customer.CustomerJsonObjectSerDer

object TestsFixtures {
  val jsonSerDer = CustomerJsonObjectSerDer()
  val testRepo = TestRepository(pgPool = pgPool)
}

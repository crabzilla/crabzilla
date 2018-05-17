package io.github.crabzilla.vertx

class ATest {

  fun main(args : Array<String>) {
    println("Hello, world!")

    val list: List<String> = existingDepartments()


  }

  fun existingDepartments(): List<String> =
    listOf("Human Resources", "Learning & Development", "Research")

}

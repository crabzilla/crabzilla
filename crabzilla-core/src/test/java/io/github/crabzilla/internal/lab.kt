// package io.github.crabzilla.internal
//
// import com.fasterxml.jackson.databind.DeserializationFeature
// import com.fasterxml.jackson.databind.SerializationFeature
// import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
// import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
// import io.github.crabzilla.core.command.Command
// import io.vertx.core.json.jackson.DatabindCodec
// import io.vertx.kotlin.core.json.jsonObjectOf
// import java.time.LocalDateTime
// import java.time.format.DateTimeFormatter
// import kotlinx.serialization.Serializable
//
// @Serializable
// class ValueObject1(val s: String, val age: Int)
//
// sealed class UserCommand : Command() {
//
//  class CreateUser(val map: Map<String, Any?>) : UserCommand() {
//    val name: String by map
//    val age: Int by map
//    val vo: ValueObject1 by map
//    val moment: LocalDateTime? by map
//  }
//
//  class ActivateUser(val map: Map<String, Any?>) : UserCommand() {
//    val name: String by map
//    val moment: LocalDateTime? by map
//  }
// }
//
// // @OptIn(ExperimentalTypeInference::class)
// // fun <T> listBuilder(@BuilderInference scope: MutableList<T>.() -> Unit): List<T> {
// //  val mutableList = mutableListOf<T>()
// //  scope.invoke(mutableList)
// //  return mutableList
// // }
//
// fun main() {
//
// //  val listNames = listBuilder {
// //    add("primeiro")
// //    add("segundo")
// //  }
//
//  val mapper = DatabindCodec.mapper()
//
//  val module = JavaTimeModule()
//  val localDateTimeDeserializer = LocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
//  module.addDeserializer(LocalDateTime::class.java, localDateTimeDeserializer)
//  mapper.registerModule(module)
//  mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
//  mapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
//  mapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
//
//  val vo = ValueObject1("teste", 53)
//
//  val create = UserCommand.CreateUser(mapOf(
//    "name" to "John Doe",
//    "age" to 25,
//    "vo" to vo,
//    "moment" to LocalDateTime.now()
//  ))
//
//  val activate = UserCommand.ActivateUser(mapOf(
//    "name" to "John Doe",
//    "moment" to LocalDateTime.now()
//  ))
//
//  println(create.map) // Prints map
//  println(create.name) // Prints "John Doe"
//  println(create.age) // Prints 25
//
//  // jackson
//
//  println("0- " + mapper.writeValueAsString(create.map))
//
//  val createUserJson = jsonObjectOf(Pair("name", "Rod"), Pair("age", 53), Pair("moment", LocalDateTime.now()))
//  println("1- " + mapper.writeValueAsString(createUserJson))
//
//  val us = UserCommand.CreateUser(createUserJson.map)
//  println("2- ${us.map}")
//  println("3- " + mapper.writeValueAsString(us.map))
//
//  listOf(create, activate)
//    .forEach { it ->
//      when (it) {
//        is UserCommand.CreateUser -> println("create")
//        is UserCommand.ActivateUser -> println("activate")
//      }
//    }
// }

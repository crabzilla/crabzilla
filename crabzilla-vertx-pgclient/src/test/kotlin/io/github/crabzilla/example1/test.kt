package io.github.crabzilla.example1


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
sealed class Project {
//  abstract val name: String
  @Serializable
  @SerialName("OwnedProject")
  data class OwnedProject(val name: String, val owner: String) : Project()
  @Serializable
  @SerialName("OwnedProject2")
  data class OwnedProject2(val name: String, val owner: Int) : Project()
}

fun main() {
  val data: Project = Project.OwnedProject("kotlinx.coroutines", "kotlin")
  val data2: Project = Project.OwnedProject2("kotlinx.coroutines", 2)
  println(Json.encodeToString(data)) // Serializing data of compile-time type Project
  println(Json.encodeToString(data2)) // Serializing data of compile-time type Project
  val lista = listOf(data, data2)
  println(Json.encodeToString(lista)) // Serializing data of compile-time type Project
}

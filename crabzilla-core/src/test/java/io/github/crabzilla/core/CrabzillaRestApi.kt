package io.github.crabzilla.core

import retrofit2.Call
import retrofit2.http.*

interface CrabzillaRestApi {

  @Headers("Content-Type: application/json")
  @POST("/{resourceId}/commands")
  fun postCommand(@Path("resourceId") resourceId: String, @Body command: Command): Call<List<String>>

  @Headers("Content-Type: application/json")
  @GET("/{resourceId}/commands/{commandId}")
  fun getUnitOfWork(@Path("resourceId") resourceId: String, @Path("commandId") commandId: String): Call<UnitOfWork>

}

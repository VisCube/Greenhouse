package com.example.myapplication

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Query

interface GreenhouseApi {
    @GET("greenhouse/data")
    fun getGreenhouseData(@Query("apiKey") apiKey: String): Call<GreenhouseData>


    @PATCH("api/devices/")
    fun updateGreenhouseData(
        @Header("API-Key") apiKey: String,
        @Body data: GreenhouseData
    ): Call<Void>

}

package com.example.myapplication.NetworkUtils

import com.example.myapplication.data.GreenhouseData
import com.example.myapplication.data.SensorReference
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface GreenhouseApi {

    @GET("devices/{id}/")
    suspend fun getGreenhouseData(@Path("id")id :String): GreenhouseData

    @PATCH("sensors/{id}/")
    fun updateGreenhouseData(@Path("id")id :Int?, @Body data: SensorReference): Call<Void>

}

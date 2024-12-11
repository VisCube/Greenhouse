package com.example.myapplication

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface GreenhouseApi {

    @GET("db") //TODO Тестовый адресс
    fun getGreenhouseData(): Call<GreenhouseData>


    @PATCH("sensors") //TODO Тестовый адресс
    fun updateGreenhouseData(@Body data: GreenhouseReferences): Call<Void>

}

package com.abhishek.nasaphotooftheday.retrofit

import com.abhishek.nasaphotooftheday.model.APODModel
import com.abhishek.nasaphotooftheday.model.APODWithDateModel
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query


interface RetrofitApiInterface {

    @GET("/planetary/apod")
    fun GetAPODResponse(@Query("api_key") api_key: String): Call<APODModel>


    @GET("/planetary/apod")
    fun GetAPODResponseWithDate(@Query("api_key") api_key: String,@Query("date") date: String): Call<APODWithDateModel>
}
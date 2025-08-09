package com.pegoku.wimp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import okhttp3.OkHttpClient
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface Ship24ApiService {
    @GET("trackers/search/{trackingNumber}/results")
    suspend fun track(
        @Header("Authorization") apiKey: String,
        @Path("trackingNumber") trackingNumber: String,
//        @Body body: TrackingRequestBody
    ): Response<TrackingResponse>

    @POST("trackers")
    suspend fun createTracker(
        @Header("Authorization") apiKey: String,
        @Body body: TrackerCreateBody
    ): Response<TrackerCreateResponse>


    @GET("couriers")
    suspend fun getCouriers(
        @Header("Authorization") apiKey: String,
        @Header("Accept") accept: String = "application/json",
    ): Response<CouriersResponse>

}
package com.example.generatar

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface MyAPI {

    @Multipart
    @POST("upload")
    fun uploadImage(
            @Part image: MultipartBody.Part,
            //@Part("image") desc: RequestBody
    ) : Call<UploadResponse>



    companion object{
        operator fun invoke() : MyAPI{
            return Retrofit.Builder()
                    .baseUrl("http://bbe10b844e46.ngrok.io/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(MyAPI::class.java)

        }
    }
}
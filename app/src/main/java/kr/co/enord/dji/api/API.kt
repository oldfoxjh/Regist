package kr.co.enord.dji.api

import kr.co.enord.dji.model.Login
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object API {
    val cApiService = CommonApiService.retrofit.create(CommonApiService::class.java)
    lateinit var iApiService : IApiService
        private set

    var loginInfo :Login? = null
        set(value) {
            value?.let {
                val host = if (it.host.last() == '/') it.host else "${it.host}/"
                val retrofit = Retrofit.Builder()
                    .baseUrl(host)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                iApiService = retrofit.create(IApiService::class.java)
            }
            field = value
        }
}
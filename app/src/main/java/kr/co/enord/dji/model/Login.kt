package kr.co.enord.dji.model

import com.google.gson.annotations.SerializedName

data class Login(
    val key:Int,
    val name:String,
    val host:String,
    val code:Int
    )

data class LoginRequestData(
    val id:String,
    val password:String
)
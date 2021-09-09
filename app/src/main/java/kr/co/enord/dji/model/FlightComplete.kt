package kr.co.enord.dji.model

import com.google.gson.annotations.SerializedName

data class FlightComplete(
    val id:Int,             // 비행경로 group_seq
    val datetime:String,                 // 착륙 일시
    val path:String                  // 비행 좌표 (위경도, 소수점 6자리까지만)
)

data class FlightStartResponse(
    val success:Boolean,
    val msg:String,
    val id: Int
)
package kr.co.enord.dji.model

data class FlightComplete(
    val code:Int,                       // 조사 작물 코드
    val group_seq:Int,             // 비행경로 group_seq
    val lat:Double,                    // 착륙 위도
    val lng:Double,                  // 착륙 경도
    val start:String,                  // 이륙 일시
    val land:String,                 // 착륙 일시
    val path:String                  // 비행 좌표 (위경도, 소수점 6자리까지만)
)

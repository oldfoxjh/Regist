package kr.co.enord.dji.model

data class GeoJsonArea(
    val no :String,
    val dist :Int,
    val num :Int,
    val right :Double,
    val top :Double,
    val left :Double,
    val bottom :Double,
    val group_seq :Int,
    val html :String,
    val land :String
)

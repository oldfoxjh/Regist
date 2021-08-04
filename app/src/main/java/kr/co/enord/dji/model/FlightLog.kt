package kr.co.enord.dji.model

import android.os.Handler
import android.util.Log
import java.util.*
import kr.co.enord.dji.DroneApplication
import retrofit2.Response
import java.text.SimpleDateFormat
import kotlin.collections.ArrayList

object FlightLog {
    private var groupSeq = -1
    private var plantCode = -1
    private var landingDateTime = Date()
    private var takeOffDateTime = Date()
    private var isFlying = false
    private var landingLat = 0.0
    private var landingLng = 0.0
    private var positions = ArrayList<String>()

    fun setMissionInfo(groupSeq:Int, plantCode:Int){
        this.groupSeq = groupSeq
        this.plantCode = plantCode
    }

    fun clearMissionInfo(){
        groupSeq = -1
        plantCode = -1
    }

    fun takeOff(){
        if (isFlying) return

        takeOffDateTime = Date()

        Handler().postDelayed({
            positions = ArrayList()
            startFlightLog()
        }, 2000)

    }


    private fun startFlightLog() {
        val droneStatus = DroneApplication.getDroneInstance().droneStatus
        if (!droneStatus.is_flying && isFlying){
            //비행하다가 착륙
            landingDateTime = Date()
            landingLat = droneStatus.drone_latitude
            landingLng = droneStatus.drone_longitude
            val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            DroneApplication.getAPI().flightComplete(FlightComplete(
                plantCode,
                groupSeq,
                landingLat,
                landingLng,
                df.format(takeOffDateTime),
                df.format(landingDateTime),
                positions.joinToString(",", prefix = "[", postfix = "]")
            )).enqueue(object: retrofit2.Callback<Void> {
                override fun onResponse(call: retrofit2.Call<Void>, response: Response<Void>) {
                    Log.e("FLIGHT LOG", "send success : $response")
                }

                override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {
                    Log.e("FLIGHT LOG", "send failure : $t")
                }
            })
        }else{
            isFlying = droneStatus.is_flying
            positions.add(String.format("[%.6f, %.6f]", droneStatus.drone_longitude, droneStatus.drone_latitude))
            Handler().postDelayed({ startFlightLog()}, 2000)
        }
    }

}

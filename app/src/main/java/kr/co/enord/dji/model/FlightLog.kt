package kr.co.enord.dji.model

import android.os.Handler
import android.util.Log
import java.util.*
import kr.co.enord.dji.DroneApplication
import kr.co.enord.dji.api.API
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import kotlin.collections.ArrayList

object FlightLog {
    private var groupSeq = -1
    private var plantCode = -1
    private var landingDateTime = Date()
    private var takeOffDateTime = Date()
    private var isFlying = false
//    private var landingLat = 0.0
//    private var landingLng = 0.0
    private var positions = ArrayList<String>()
    private var id = -1

    fun setMissionInfo(groupSeq:Int, plantCode:Int){
        this.groupSeq = groupSeq
        this.plantCode = plantCode
    }

    fun clearMissionInfo(){
        groupSeq = -1
        plantCode = -1
        id = -1
    }

    fun takeOff(){
        if (isFlying) return

        takeOffDateTime = Date()

        sendFlightStart()

        Handler().postDelayed({
            positions = ArrayList()
            startFlightLog()
        }, 2000)

    }

    private fun sendFlightStart(){
        val droneStatus = DroneApplication.getDroneInstance().droneStatus
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        API.iApiService.flightStart(groupSeq, droneStatus.home_latitude, droneStatus.home_longitude, df.format(
            takeOffDateTime),API.loginId).enqueue(object : Callback<FlightStartResponse>{
            override fun onResponse(call: Call<FlightStartResponse>, response: Response<FlightStartResponse>) {
                Log.i("FLIGHT START", "send success : $response")
                response.body()?.let {
                    id = it.id
                }
            }

            override fun onFailure(call: Call<FlightStartResponse>, t: Throwable) {
                Log.e("FLIGHT START", "send failure : $t")
            }

        })
    }

    private fun startFlightLog() {
        DroneApplication.getDroneInstance().droneStatus?.let{ droneStatus ->
            if (!droneStatus.is_flying && isFlying){
                //비행하다가 착륙
                isFlying = false
                landingDateTime = Date()
                val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                API.iApiService.flightComplete(FlightComplete(
                    id,
                    df.format(landingDateTime),
                    positions.joinToString(",", prefix = "[", postfix = "]")
                )).enqueue(object: retrofit2.Callback<Void> {
                    override fun onResponse(call: retrofit2.Call<Void>, response: Response<Void>) {
                        Log.i("FLIGHT LOG", "send success : $response")
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

}

package kr.co.enord.dji.model

import android.os.Environment
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

object GeoJsonEx {

    private val directoryPath = Environment.getExternalStorageDirectory().toString() + File.separator + "enord" + File.separator + "Mission"+ File.separator + "중단임무"
    private val fileName =  "temp.gson"


    private var json = JSONObject()
    private var deletedArrayCount = 0;
    private val consumedIndex = ArrayList<Int>()
    fun setJSON(path: String){
        val jsonText = File(path).readText()
        json = JSONObject(jsonText)
        deletedArrayCount = 0;
        consumedIndex.clear()
    }
    fun deleteIndex(idx: Int) {
        try {
            if (consumedIndex.contains(idx)) return //한번 들어왔던 index값은 리턴
            consumedIndex.add(idx)
            val startIndex = idx - 2 //진행되는 포인트가 들어오므로 완료된것은 -1, 시작 포인트가 젤처음에 추가되므로 -1 도합 -2가 완료된 배열의 인덱스
            if (startIndex < 0) return

            var calculatedIdx = startIndex - deletedArrayCount
            val features: JSONArray = json.getJSONArray("features")
            var type: String = json.getString("type").lowercase(Locale.getDefault())

            if (type != "featurecollection") return

            // Feature의 첫번째 이외의 Data는 무시
            val geometry = features.getJSONObject(0).getJSONObject("geometry")

            type = geometry.getString("type").lowercase(Locale.getDefault())
            var coordinates: JSONArray? = null
            if (type == "linestring") {
                coordinates = geometry.getJSONArray("coordinates")
            } else if (type == "polygon") {
                coordinates = geometry.getJSONArray("coordinates").getJSONArray(0)
            }

            for (i in 0..calculatedIdx) {
                coordinates?.remove(0)
                ++deletedArrayCount

                Log.e(
                    "DJI",
                    "target idx : " + idx + " delete count : " + deletedArrayCount + " remain count : " + coordinates?.length()
                )
            }

        } catch (e: Exception) {

        }
    }

    fun deleteIndex(idx: List<GeoPoint>){
        try {
            val features: JSONArray = json.getJSONArray("features")
            var type: String = json.getString("type").lowercase(Locale.getDefault())

            if (type != "featurecollection") return

            // Feature의 첫번째 이외의 Data는 무시
            val geometry = features.getJSONObject(0).getJSONObject("geometry")

            type = geometry.getString("type").lowercase(Locale.getDefault())
            var coordinates: JSONArray = JSONArray()
            if (type == "linestring") {
                coordinates = geometry.getJSONArray("coordinates")
            } else if (type == "polygon") {
                coordinates = geometry.getJSONArray("coordinates").getJSONArray(0)
            }

            for (item in idx) {
                // 비교
                var index = -1;
                for (i in 0 until coordinates.length()) {
                    val obj = coordinates.getJSONArray(i)
                    var distance = item.distanceToAsDouble(GeoPoint(obj.getDouble(1), obj.getDouble(0)))

                    if (distance < 2) {
                        index = i
                        break
                    };
                }
                if (index > -1) coordinates.remove(index);
            }

            Log.e("DJI", "count : " + coordinates.length())

        } catch (e: Exception) {

        }
    }

    fun saveToFile(){
        val file = File(directoryPath)
        file?.mkdirs()
        val jsonString = json.toString()

        val file2 = File(file, fileName)
        if (file2.exists()) file2.delete()

        File(file, fileName).writeText(jsonString)
    }
}
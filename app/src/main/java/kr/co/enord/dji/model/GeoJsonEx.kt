package kr.co.enord.dji.model

import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object GeoJsonEx {

    private val directoryPath = Environment.getExternalStorageDirectory().toString() + File.separator + "enord" + File.separator + "중단임무"
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
    fun deleteIndex(idx: Int){
        try {
            if(consumedIndex.contains(idx)) return //한번 들어왔던 index값은 리턴
            consumedIndex.add(idx)
            val startIndex = idx - 2 //진행되는 포인트가 들어오므로 완료된것은 -1, 시작 포인트가 젤처음에 추가되므로 -1 도합 -2가 완료된 배열의 인덱스
            if (startIndex < 0) return

            var calculatedIdx = startIndex - deletedArrayCount
            val features: JSONArray = json.getJSONArray("features")
            var type: String = json.getString("type").toLowerCase()

            if (type != "featurecollection") return

            // Feature의 첫번째 이외의 Data는 무시
            val geometry = features.getJSONObject(0).getJSONObject("geometry")

            type = geometry.getString("type").toLowerCase()
            var coordinates: JSONArray? = null
            if (type == "linestring") {
                coordinates = geometry.getJSONArray("coordinates")
            } else if (type == "polygon") {
                coordinates = geometry.getJSONArray("coordinates").getJSONArray(0)
            }
            for (i in 0 .. calculatedIdx){
                coordinates?.remove(0)
                ++deletedArrayCount
            }

        }catch (e:Exception){}
    }

    fun saveToFile(){
        val file = File(directoryPath)
        if (!file?.mkdirs()) {
            return
        }
        val jsonString = json.toString()
        File(file, fileName).writeText(jsonString)
    }

}
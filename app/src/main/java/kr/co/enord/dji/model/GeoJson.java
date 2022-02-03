package kr.co.enord.dji.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class GeoJson {
    public static final int GEO_TYPE_NONE = 0x00;
    public static final int GEO_TYPE_LINE = 0x01;
    public static final int GEO_TYPE_POLYGON = 0x02;

    private int m_type = GEO_TYPE_NONE;
    private List<GeoPoint> m_coordinates = new ArrayList<>();
    private RectD m_bound = null;
    private List<Boolean> mTakePictureSpot = new ArrayList<>();

    //서버에서 내려받을 경우 unique key와 history여부(찍은곳인지 아닌지)가 추가됨
    private int groupSeq = -1;
    private String landTime = "";

    private JSONObject originalJSONObject = null;

    public JSONObject getOriginalJSONObject() {
        return originalJSONObject;
    }

    public GeoJson(String json) {
        try {
            // FeatureCollection 인지 확인
            JSONObject geo = new JSONObject(json);
            originalJSONObject = geo;

            JSONArray features = geo.getJSONArray("features");
            String type = geo.getString("type").toLowerCase();

            if(!type.equals("featurecollection")) return;

            //Feature에서 property의 group_seq, land_datetime 가져옴(서버에서 받은것)
            JSONObject properties = features.getJSONObject(0).optJSONObject("properties");
            if(properties != null) {
                groupSeq = properties.optInt("group_seq");
                landTime = properties.optString("land_datetime");
            }

            // Feature의 첫번째 이외의 Data는 무시
            JSONObject geometry = features.getJSONObject(0).getJSONObject("geometry");

            type = geometry.getString("type").toLowerCase();
            JSONArray coordinates = null;
            if(type.equals("linestring")){
                m_type = GEO_TYPE_LINE;
                coordinates = geometry.getJSONArray("coordinates");
            }else if(type.equals("polygon")){
                m_type = GEO_TYPE_POLYGON;
                coordinates = geometry.getJSONArray("coordinates").getJSONArray(0);
            }

            // 좌표정보를 GeoPoint로 변환
            for(int i = 0; i < coordinates.length(); i++){
                JSONArray point = coordinates.getJSONArray(i);

                double longitude = point.getDouble(0);
                double latitude = point.getDouble(1);
                double altitude = 0;
                if(point.length() > 2){
                    altitude = point.getDouble(2);
                }

                m_coordinates.add(new GeoPoint(latitude, longitude, altitude));

                try {
                    //point의 4번째 숫자가 0이면 사진촬영안함
                    mTakePictureSpot.add(point.getInt(3) != 0);
                }catch (JSONException e){
                    mTakePictureSpot.add(true);
                }

            }

            m_bound = new RectD(m_coordinates);
        } catch (JSONException e) {

        }
    }

    public int getGeoType() {
        return m_type;
    }

    public List<GeoPoint> getCoordinates() {
        return m_coordinates;
    }

    public List<GeoPoint> getBound(){
        return m_bound.getBound();
    }

    public GeoPoint getCenter(){
        return m_bound.getCenter();
    }

    public int getGroupSeq() {
        return groupSeq;
    }

    public String getLandTime() {return  landTime;}

    public Boolean fromServer(){
        return groupSeq != -1;
    }

    public List<Boolean> getTakePictureSpot() {
        return mTakePictureSpot;
    }
    @Override
    public String toString() {
        return "GeoJson{" +
                "m_type=" + m_type +
                ", m_coordinates=" + m_coordinates.size() +
                '}';
    }
}

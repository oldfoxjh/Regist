package kr.co.enord.dji.api;

import com.google.gson.JsonArray;

import kr.co.enord.dji.model.FlightComplete;
import kr.co.enord.dji.model.FlightStartResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface IApiService {

//    @Multipart
//    @POST("upload.php")
//    Call<ServerResponse> uploadFileWithParam(@Part MultipartBody.Part file, @PartMap() Map<String, RequestBody> partMap);

//    /**
//     * 2.경로 지역 목록
//     */
//    @GET("api/mobile/areas/{latitude}/{longitude}/{targetCode}")
//    Call<List<GeoJsonArea>> listInArea(@Path("latitude") Double lat, @Path("longitude") Double lng, @Path("targetCode") int target);


    /**
     * 이륙지점 검색
     */
    @GET("takeoff/{query}")
    Call<JsonArray> takeOffPoints(@Path("query") String keyword);

    /**
     * 4.비행경로 요청
     */
    @GET("areas/{latitude}/{longitude}/{targetCode}")
    Call<JsonArray> flightPlan(@Path("latitude") Double lat, @Path("longitude") Double lng, @Path("targetCode") int target);

    /**
     * 비행시작
     */
    @GET("flight/start/{group_seq}/{lat}/{lng}/{datetime}/{id}")
    Call<FlightStartResponse> flightStart(@Path("group_seq") int groupSeq, @Path("lat") Double lat, @Path("lng") Double lng, @Path("datetime") String date, @Path("id") String loginid);

    /**
     * 5.완료
     */
    @POST("flight/complete")
    Call<Void> flightComplete(@Body FlightComplete body);
}

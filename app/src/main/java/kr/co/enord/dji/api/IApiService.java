package kr.co.enord.dji.api;

import com.google.gson.JsonObject;
import kr.co.enord.dji.model.AltResponse;
import kr.co.enord.dji.model.FlightComplete;
import kr.co.enord.dji.model.GeoJsonArea;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

import java.util.List;

public interface IApiService {
    Retrofit retrofit = new Retrofit.Builder()
//            .baseUrl("http://enord.iptime.org:9998/")
            .baseUrl("http://sky.farmmap.kr/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

//    @Multipart
//    @POST("upload.php")
//    Call<ServerResponse> uploadFileWithParam(@Part MultipartBody.Part file, @PartMap() Map<String, RequestBody> partMap);

    /**
     * 3.고도 요청
     */
    @GET("api/mobile/alt/{latitude}/{longitude}/")
    Call<AltResponse> altitude(@Path("longitude") Double longitude, @Path("latitude") Double latitude);

    /**
     * 1. 경로 종류 코드받아오기
     */
    @GET("api/mobile/targets")
    Call<JsonObject> targets();

    /**
     * 2.경로 지역 목록
     */
    @GET("api/mobile/areas/{latitude}/{longitude}/{targetCode}")
    Call<List<GeoJsonArea>> listInArea(@Path("latitude") Double lat, @Path("longitude") Double lng, @Path("targetCode") int target);

    /**
     * 4.비행경로 요청
     */
    @GET("api/mobile/points/{latitude}/{longitude}/{targetCode}/{groupSeq}")
    Call<JsonObject> flightPlan(@Path("latitude") Double lat, @Path("longitude") Double lng, @Path("targetCode") int target, @Path("groupSeq") int groupSeq);

    /**
     * 5.완료
     */
    @POST("api/mobile/flight/complete")
    Call<Void> flightComplete(@Body FlightComplete body);
}

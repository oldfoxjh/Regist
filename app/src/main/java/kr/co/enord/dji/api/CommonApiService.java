package kr.co.enord.dji.api;

import java.util.List;

import kr.co.enord.dji.model.AltResponse;
import kr.co.enord.dji.model.Login;
import kr.co.enord.dji.model.LoginRequestData;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface CommonApiService {
    Retrofit retrofit = new Retrofit.Builder()
//            .baseUrl("http://enord.iptime.org:9998/")
            .baseUrl("http://sky.farmmap.kr/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

//    @Multipart
//    @POST("upload.php")
//    Call<ServerResponse> uploadFileWithParam(@Part MultipartBody.Part file, @PartMap() Map<String, RequestBody> partMap);


    /**
     * 로그인
     */

    @POST("api/common/login")
    Call<List<Login>> login(@Body LoginRequestData data);

    /**
     * 3.고도 요청
     */
    @GET("api/common/alt/{latitude}/{longitude}/")
    Call<AltResponse> altitude(@Path("longitude") Double longitude, @Path("latitude") Double latitude);

}

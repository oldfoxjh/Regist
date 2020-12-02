package kr.co.enord.dji.api;

import java.util.Map;

import kr.co.enord.dji.model.ServerResponse;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;

public interface IApiService {
    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://enord.iptime.org:9099/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    @Multipart
    @POST("upload.php")
    Call<ServerResponse> uploadFileWithParam(@Part MultipartBody.Part file, @PartMap() Map<String, RequestBody> partMap);
}

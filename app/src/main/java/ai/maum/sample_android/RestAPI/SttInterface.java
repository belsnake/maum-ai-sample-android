package ai.maum.sample_android.RestAPI;

import ai.maum.sample_android.Model.SttResponse;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface SttInterface {

    @Multipart
    @POST("/api/stt/")
    Call<SttResponse> exec(
            @Part("ID") RequestBody ID,
            @Part("key") RequestBody key,
            @Part("lang") RequestBody lang,
            @Part("sampling") RequestBody sampling,
            @Part("level") RequestBody level,
            @Part("cmd") RequestBody cmd,
            @Part MultipartBody.Part file
    );
}

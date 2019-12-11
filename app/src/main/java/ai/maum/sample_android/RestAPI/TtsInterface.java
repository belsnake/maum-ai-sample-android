package ai.maum.sample_android.RestAPI;

import ai.maum.sample_android.Model.TtsRequest;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface TtsInterface {

    @Headers("Content-Type: application/json")
    @POST("/tts/stream/")
    Call<ResponseBody> exec(@Body TtsRequest body);
}

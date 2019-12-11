package ai.maum.sample_android.RestAPI;


import ai.maum.sample_android.Model.MrcRequest;
import ai.maum.sample_android.Model.MrcResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface MrcInterface {

    @Headers("Content-Type: application/json")
    @POST("/api/bert.mrc/")
    Call<MrcResponse> exec(@Body MrcRequest body);
}

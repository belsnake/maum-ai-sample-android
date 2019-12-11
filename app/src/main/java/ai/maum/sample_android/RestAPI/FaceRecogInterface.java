package ai.maum.sample_android.RestAPI;

import ai.maum.sample_android.Model.FaceRecogResponse;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;

public interface FaceRecogInterface {

    @Multipart
    @POST("/insight/app/recogFace/")
    Call<FaceRecogResponse> recogFace(
            @Part("apiId") RequestBody apiId,
            @Part("apiKey") RequestBody apiKey,
            @Part("dbId") RequestBody dbId,
            @Part MultipartBody.Part file
    );

    @Multipart
    @PUT("/insight/app/setFace/")
    Call<FaceRecogResponse> setFace(
            @Part("apiId") RequestBody apiId,
            @Part("apiKey") RequestBody apiKey,
            @Part("dbId") RequestBody dbId,
            @Part("faceId") RequestBody faceId,
            @Part MultipartBody.Part file
    );

    @Multipart
    @DELETE("/insight/app/deleteFace/")
    Call<FaceRecogResponse> delFace(
            @Part("apiId") RequestBody apiId,
            @Part("apiKey") RequestBody apiKey,
            @Part("dbId") RequestBody dbId,
            @Part("faceId") RequestBody faceId
    );
}

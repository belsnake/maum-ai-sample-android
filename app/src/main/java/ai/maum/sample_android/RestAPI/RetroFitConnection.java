package ai.maum.sample_android.RestAPI;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetroFitConnection {
    String URL = "https://api.maum.ai/"; // 서버 API

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    public MrcInterface mrc = retrofit.create(MrcInterface.class);
    public TtsInterface tts = retrofit.create(TtsInterface.class);
    public SttInterface stt = retrofit.create(SttInterface.class);
    public FaceRecogInterface faceRecog = retrofit.create(FaceRecogInterface.class);
}

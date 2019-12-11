package ai.maum.sample_android.Sample;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.BinderThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;

import ai.maum.sample_android.Model.TtsRequest;
import ai.maum.sample_android.R;
import ai.maum.sample_android.RestAPI.RetroFitConnection;
import ai.maum.sample_android.Utils;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Tts_Activity extends AppCompatActivity {
    final String TAG = "TTS";

    String[] mSpeaker = new String[] {
            "baseline_kor",
            "baseline_eng",
            "kor_kids_m",
            "kor_kids_f"
    };

    @BindView(R.id.Spinner)
    Spinner mSpinner;

    @BindView(R.id.Edit_Sentence)
    EditText mEdit_Sentence;

    String mFilePath_Result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tts___activity);
        ButterKnife.bind(this);

        init();
    }

    void init() {
        /* 화자 선택 관련 설정 */
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.tts_speaker, android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);

        /* 수신될 음원 저장 파일 경로 설정 */
        mFilePath_Result = Environment.getExternalStorageDirectory() + "/maum.ai/tts/tts_result.wav";
    }

    /*
     ** TTS 변환 실행
     */
    @OnClick(R.id.Button_Exec)
    public void OnClick_Exec() {

        /* 파라메타 설정 */
        TtsRequest request = new TtsRequest();
        request.setApiId(Utils.getMetaData(this, "api_id"));
        request.setApiKey(Utils.getMetaData(this, "api_key"));
        request.setText(mEdit_Sentence.getText().toString());
        request.setVoiceName(mSpeaker[mSpinner.getSelectedItemPosition()]);

        /* TTS RestAPI 실행 */
        RetroFitConnection conn = new RetroFitConnection();
        Call<ResponseBody> call = conn.tts.exec(request);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Gson gson = new Gson();
                if (response.isSuccessful()) {
                    // 성공적으로 서버에서 데이터 불러옴.
                    Log.d(TAG, "onResponse: succ : ");

                    boolean state = Utils.writeResponseBodyToDisk(response.body(), mFilePath_Result);
                    if(state == false) {
                        Log.e(TAG, "==> file writing fail");
                        return;
                    }

                    startMedia(mFilePath_Result);
                } else {
                    // 서버와 연결은 되었으나, 오류 발생
                    Log.d(TAG, "onResponse: fail"); //서버와 연결 실패
                }
            };

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.toString()); //서버와 연결 실패
            }
        });
    }

    /* ============================================================================================= */
    // 미디어 플레이어 설정
    /* ============================================================================================= */

    MediaPlayer mMediaPlayer = null;

    void startMedia(String filepath) {
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(filepath);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mMediaPlayer.reset();
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

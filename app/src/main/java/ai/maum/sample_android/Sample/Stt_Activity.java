package ai.maum.sample_android.Sample;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import ai.maum.sample_android.Model.SttResponse;
import ai.maum.sample_android.R;
import ai.maum.sample_android.RestAPI.RetroFitConnection;
import ai.maum.sample_android.Utils;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Stt_Activity extends AppCompatActivity {
    final String TAG = "STT";

    String  mFilePath_Record;

    @BindView(R.id.Text_Result)
    TextView mText_Result;

    @BindView(R.id.Radio_Korean)
    RadioButton mRadio_Korean;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stt___activity);
        ButterKnife.bind(this);

        // UI 초기 설정
        init();
    }

    /* ============================================================================================= */
    // 초기 데이타 설정
    /* ============================================================================================= */
    private void init() {
        mFilePath_Record = Environment.getExternalStorageDirectory() + "/maum.ai/stt/stt.wav";
    }


    /* ============================================================================================= */
    // 컨트롤 버튼
    //    녹음 > 중단
    /* ============================================================================================= */

    @BindView(R.id.Button_Exec)
    Button mButton_Exec;

    /*
     ** TTS 변환 실행
     */
    @OnClick(R.id.Button_Exec)
    public void OnClick_Exec() {
        if(mButton_Exec.getText().toString().equals("녹음")) {
            recordAudio();
            mButton_Exec.setText("멈춤");
        }
        else {
            stopRecording();
            mButton_Exec.setText("녹음");
            mButton_Exec.setEnabled(false);

            execAPI();
        }
    }

    /*
     ** REST API 실행
     */
    private void execAPI() {
        /* 파라메타 설정 */
        File file = new File(mFilePath_Record);
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part uploadFile = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        RequestBody id = RequestBody.create(MediaType.parse("text/plain"), getString(R.string.api_id));
        RequestBody key = RequestBody.create(MediaType.parse("text/plain"), getString(R.string.api_key));
        RequestBody lang = RequestBody.create(MediaType.parse("text/plain"), (mRadio_Korean.isChecked() ? "kor" : "eng"));
        RequestBody sampling = RequestBody.create(MediaType.parse("text/plain"), "16000");
        RequestBody level = RequestBody.create(MediaType.parse("text/plain"), "baseline");
        RequestBody cmd = RequestBody.create(MediaType.parse("text/plain"), "runFileStt");

        /* TTS RestAPI 실행 */
        RetroFitConnection conn = new RetroFitConnection();
        Call<SttResponse> call = conn.stt.exec( id, key, lang, sampling, level, cmd, uploadFile );
        call.enqueue(new Callback<SttResponse>() {
            @Override
            public void onResponse(Call<SttResponse> call, Response<SttResponse> response) {
                Gson gson = new Gson();
                if (response.isSuccessful()) {
                    // 성공적으로 서버에서 데이터 불러옴.
                    Log.d(TAG, "onResponse: succ : " + response.body().toString());

                    mText_Result.setText(response.body().getData());
                } else {
                    // 서버와 연결은 되었으나, 오류 발생
                    Log.d(TAG, "onResponse: fail >> " + response.toString()); //서버와 연결 실패
                }

                mButton_Exec.setText("녹음");
                mButton_Exec.setEnabled(true);
            };

            @Override
            public void onFailure(Call<SttResponse> call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.toString()); //서버와 연결 실패

                mButton_Exec.setText("녹음");
                mButton_Exec.setEnabled(true);
            }
        });
    }



    /* ============================================================================================= */
    // 녹음
    /* ============================================================================================= */

    private final int SAMPLE_RATE = 16000;
    private final int CHANNEL_MONO = AudioFormat.CHANNEL_IN_MONO;
    private final int ENCODING_FMT = AudioFormat.ENCODING_PCM_16BIT;
    private final int BUFFER_SIZE = 1024;
    private final int BIT_DEPTH = 16;
    private final int CH_NUM = 1;

    AudioRecord mRecorder;
    boolean mIsRecording = false;
    Thread mThread_Record = null;

    /*
     ** 녹음 시작
     */
    private void recordAudio() {
        /* 오디오 녹음 객체 생성 */
        mRecorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_MONO,
                ENCODING_FMT,
                BUFFER_SIZE
        );

        /* 녹음 시작 */
        mRecorder.startRecording();
        mIsRecording = true;

        /* 실제 레코딩 수행할 쓰레드 기동 */
        mThread_Record = new Thread(mRunnable_Record);
        mThread_Record.start();
    }

    /*
     ** 녹음 종료
     */
    private void stopRecording() {
        /* 쓰레드가 종료될 수 있도록 상태 설정 */
        mIsRecording = false;

        /* 쓰레드가 종료할 때까지 대기 */
        try {
            if(mThread_Record != null) mThread_Record.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /* 자원 해지 */
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }

        /* 다시 녹음 가능하도록 */
        mButton_Exec.setEnabled(true);
    }

    /*
     ** 마이크 입력을 받아서 파일로 저장
     */
    Runnable mRunnable_Record = new Runnable() {
        @Override
        public void run() {

            File file = new File(mFilePath_Record);
            file.delete();

            File dir = new File(file.getParent());
            dir.mkdirs();

            byte[] readData = new byte[BUFFER_SIZE];
            FileOutputStream fos = null;

            /* 파일 생성 */
            try {
                fos = new FileOutputStream(mFilePath_Record);
            } catch(FileNotFoundException e) {
                e.printStackTrace();
            }

            /* 오디오 파일 기록 */
            try {
                // WAV 헤드 기록
                Utils.writeWavHeader(fos, (short) CH_NUM, SAMPLE_RATE, (short) BIT_DEPTH);

                // 오디오 기록
                while(mIsRecording) {
                    int ret = mRecorder.read(readData, 0, BUFFER_SIZE);  //  AudioRecord의 read 함수를 통해 pcm data 를 읽어옴
                    Log.d(TAG, "read bytes is " + ret);

                    fos.write(readData, 0, BUFFER_SIZE);    //  읽어온 readData 를 파일에 write 함
                }

                // WAV 헤드 업데이트
                Utils.updateWavHeader(file);

            }catch (IOException e){
                e.printStackTrace();
            }

            /* 자원 해지 */
            try {
                fos.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    };
}

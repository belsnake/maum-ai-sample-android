package ai.maum.sample_android.Sample;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import ai.maum.sample_android.Main_Activity;
import ai.maum.sample_android.R;
import ai.maum.sample_android.Utils;
import ai.maum.sample_android.mAIEng.PhonicsResult;
import ai.maum.sample_android.mAIEng.PronResult;
import ai.maum.sample_android.mAIEng.ResCode;
import ai.maum.sample_android.mAIEng.SttResult;
import ai.maum.sample_android.mAIEng.mAIEngHandler;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class EngEduStt_Activity extends AppCompatActivity {

    @BindView(R.id.Layer_Result)
    LinearLayout mLayer_Result;

    @BindView(R.id.Edit_Sentence)
    EditText mEdit_Sentence;

    @BindView(R.id.Text_UserText)
    TextView mText_User;

    @BindView(R.id.Text_Score)
    TextView mText_Score;

    @BindView(R.id.Button_Play)
    Button mButton_Play;

    @BindView(R.id.Button_Exec)
    Button mButton_Exec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.eng_edu_stt___activity);
        ButterKnife.bind(this);

        initEngEdu();
    }


    /* 인식 실행 */
    @OnClick(R.id.Button_Exec)
    public void OnClick_Exec() {
        mAIHandler.startStt("userid", "customer1", mEdit_Sentence.getText().toString());
        mButton_Exec.setText("음성인식중..");
        mButton_Exec.setEnabled(false);
    }


    /* 사용자 발화 재생 */
    @OnClick(R.id.Button_Play)
    public void OnClick_Play() {
        startMedia((String) mButton_Play.getTag());
    }


    /* ===================================================================================================================== */
    // 영어교육 Handler
    /* ===================================================================================================================== */

    mAIEngHandler mAIHandler;

    void initEngEdu() {
        mAIHandler = new mAIEngHandler(this);
        mAIHandler.init(Utils.getMetaData(this, "api_id"), Utils.getMetaData(this, "api_key"));
        mAIHandler.setCallback(mSttCallback);
    }

    /*
    ** 영어교육 이벤트 콜백
    */
    mAIEngHandler.mAIEngCallback mSttCallback = new mAIEngHandler.mAIEngCallback() {
        SttResult sttResult;

        /* 영어교육 인식 시작 콜백 */
        @Override
        public void onStart(mAIEngHandler handler) {
            Log.e("AAA", "@@@ SttHandler......................onStart()");
        }

        /* 영어교육 인식 취소 콜백 */
        @Override
        public void onCancel(mAIEngHandler handler) {
            Log.e("AAA", "@@@ SttHandler......................onCancel()");
        }

        /* 발음평가, 파닉스 평가 호출 시, STT 인식 완료시에 알림 콜백 */
        @Override
        public void onNotiStt(mAIEngHandler handler, SttResult result) {
            Log.e("AAA", "@@@ SttHandler......................onNotiStt()");

            sttResult = result;
        }

        /* 영어교육 STT 인식 호출시 완료 콜백 */
        @Override
        public void onSuccess(mAIEngHandler handler, SttResult result) {
            Log.e("AAA", "@@@ SttHandler......................onSuccess()");

            mLayer_Result.setVisibility(View.VISIBLE);
            mText_User.setText(result.getResultText());
            mText_Score.setText("" + result.getSentenceScore());
            mButton_Play.setTag(result.getRecordUrl());

            mButton_Exec.setText("녹음(STT 실행)");
            mButton_Exec.setEnabled(true);
        }

        /* 발음평가 호출시 완료 콜백 */
        @Override
        public void onSuccess(mAIEngHandler handler, PronResult result) {
            Log.e("AAA", "@@@ SttHandler......................onSuccess()");
        }

        /* 파닉스평가 호출시 완료 콜백 */
        @Override
        public void onSuccess(mAIEngHandler handler, PhonicsResult result) {
            Log.e("AAA", "@@@ SttHandler......................onSuccess()");
        }

        @Override
        public void onError(mAIEngHandler handler, int reason, String message) {
            Log.e("AAA", "@@@ SttHandler......................onError() : " + message);

            Toast.makeText(EngEduStt_Activity.this, message, Toast.LENGTH_LONG).show();

            mButton_Exec.setText("녹음(STT 실행)");
            mButton_Exec.setEnabled(true);
        }

        @Override
        public void onProgress(mAIEngHandler handler, float pitch) {
        }
    };


    /* ===================================================================================================================== */
    // 미디어 재생
    /* ===================================================================================================================== */

    MediaPlayer mMediaPlayer;

    void startMedia(String sound_url) {
        try {
            stopMedia();
            mMediaPlayer = MediaPlayer.create(this, Uri.parse(sound_url));
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.e("AAA", "Prepared sound");
                    mMediaPlayer.start();
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void stopMedia() {
        if(mMediaPlayer == null) return;

        if(mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();

        }

        mMediaPlayer.reset();
        mMediaPlayer.release();
        mMediaPlayer = null;
    }
}

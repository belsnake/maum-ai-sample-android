package ai.maum.sample_android.mAIEng;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.internal.LinkedTreeMap;

import java.util.List;
import java.util.Map;


import ai.maum.sample_android.Utils;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class mAIEngHandler {
    final String BASE_URL = "wss://maieng.maum.ai:7777/engedu/v1/websocket";

    final int RESPONSE_TIMEOUT = 10*1000;   // 10초
    final int EPD_TIMEOUT = 800;            // msec

    final int PITCH___BEGIN = 1000;          // EPD 검출 시작을 위한 최소 피치
    final int PITCH___END = 90;             // EPD 검출 마지막을 위한 최소 피치

    final int MODE___NONE = 0;
    final int MODE___STT = 1;
    final int MODE___PRON = 2;
    final int MODE___PHONICS = 3;
    int mMode = MODE___NONE;


    private Context mContext;

    private AudioDispatcher mAudioDispatcher = null;

    private String mApiId, mApiKey;

    OkHttpClient mClient = null;
    WebSocket mWebSocket = null;

    private boolean mIsRunning = false;         // 현재 서비스가 동작중인지 여부
    private boolean mIsSpeaking = false;        // 실제 사용자 발화중인지 여부 (잔소음은 무시)
    private boolean mIsStreaming = false;       // STT를 위한 음성 데이타 전송 여부
    private boolean mIsNormalClosing = false;   // 정상 종료 여부
    private boolean mIsStartingAudio = false;   // 오디오 입력 시작 여부

    /* ============================================================================================================= */
    // 생성 및 초기화
    /* ============================================================================================================= */

    public mAIEngHandler(Context context) {
        mContext = context;
    }

    public void init(String api_id, String api_key) {
        mApiId = api_id;
        mApiKey = api_key;
    }

    /* ============================================================================================================= */
    // 서비스 제어
    /* ============================================================================================================= */

    /* 사용자 발화를 텍스트 변환, 문장 평가 포함 */
    public void startStt(String user_id, String model, String answer_text) {
        if(mIsRunning) return;

        mMode = MODE___STT;
        start(user_id, model, answer_text, BASE_URL + "/stt");
    }

    /* STT + 발음 평가 */
    public void startPron(String user_id, String model, String answer_text) {
        if(mIsRunning) return;

        mMode = MODE___PRON;
        start(user_id, model, answer_text, BASE_URL + "/pron");
    }

    /* STT + 파닉스 평가 */
    public void startPhonics(String user_id, String model, String answer_text, String chksym) {
        if(mIsRunning) return;

        mMode = MODE___PHONICS;
        start(user_id, model, answer_text, BASE_URL + "/phonics");
    }

    /* mAIEng API 서버와 연동 */
    void start(String user_id, String model, String answer_text, String wss_url) {
        mIsRunning = true;              // 서비스 동작 중
        mIsStreaming = true;            // 스트리밍 전송 중
        mIsSpeaking = false;            // 발화중 아님
        mIsNormalClosing = false;       // 서버로부터의 연결 종료와 내부에서 연결 종료시를 구분하기 위해 사용
                                        // (비정상종료=서버에서 단절)
        mIsStartingAudio = false;

        startTimeout();
        connect(user_id, model, answer_text, wss_url);
        openAudio();
    }

    /* 진행중인 기능 취소 */
    public void cancel() {

        mIsNormalClosing = true;
        closeAudio();
        disconnect();
        cancelTimeout();

        mIsRunning = false;

        if(mCallback != null) mCallback.onCancel(this);
    }

    /*
    ** 작업 완료
    **
    ** @ flag_normal : 정상 종료 여부 설정.
    */
    public void shutdown() {
        mIsNormalClosing = true;    // WS.onFailure() 상에서 mCallback.onError()가 호출되지 않도록 한다.
        closeAudio();
        disconnect();
        cancelTimeout();

        mIsRunning = false;
    }

    /* ============================================================================================================= */
    // 상태 조회
    /* ============================================================================================================= */

    public boolean isRunning() {
        return mIsRunning;
    }


    /* ============================================================================================================= */
    // 타임아웃
    /* ============================================================================================================= */

    void startTimeout() {
        mHandler.postDelayed(mRunnable_UserTimeout, RESPONSE_TIMEOUT);
    }

    void cancelTimeout() {
        mHandler.removeCallbacks(mRunnable_UserTimeout);
    }

    Runnable mRunnable_UserTimeout = new Runnable() {
        @Override
        public void run() {
            Log.e("AAA", "@ TIMEOUT ....................................... mAIEngHandler");

            shutdown();
            if(mCallback != null) mCallback.onError(mAIEngHandler.this, ResCode.FAIL___TIMEOUT, "response timeout");
        }
    };




    /* ============================================================================================================= */
    // 서버 연동
    /* ============================================================================================================= */

    /*
    ** ENGEDU API 서버와 연결
    */
    void connect(String user_id, String model, String answer_text, String wss_url) {
        /* 웹소켓 설정 및 연결 */
        String param = String.format("?apiId=%s&apiKey=%s&userId=%s&model=%s&answerText=%s",
                mApiId,
                mApiKey,
                user_id,
                model,
                answer_text);

        mClient = HttpUtils.getUnsafeOkHttpClient().build();
        Request request = new Request.Builder().url(wss_url + param).build();
        mWebSocket = mClient.newWebSocket(request, mWSListener);
        Log.e("AAA", "@ WS ............................ mWebSocket = " + mWebSocket);
        mClient.dispatcher().executorService().shutdown();
    }

    /*
    ** ENGEDU API 서버와 단절
    */
    void disconnect() {
        if(mWebSocket == null) return;
        try {
            mWebSocket.cancel();
        } catch (Exception e) {}

        mWebSocket = null;
        mClient = null;
    }

    /*
    **
    */
    WebSocketListener mWSListener = new WebSocketListener() {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            super.onOpen(webSocket, response);
            Log.e("AAA", "@ WS ............................ onOpen()");
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            super.onMessage(webSocket, text);
            Log.e("AAA", "@ WS ..... onMessage(text) >> " + text);

            try {
                Map<String, Object> map = Utils.jsonString2Map(text);
                final Map<String, Object> result_map = (Map) map.get("result");

                switch(((Double) map.get("resCode")).intValue()) {
                    case ResCode.SUCC:
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(mMode == MODE___STT) procResultStt(ResCode.SUCC, result_map);
                                else if(mMode == MODE___PRON) procResultPron(result_map);
                                else if(mMode == MODE___PHONICS) procResultPhonics(result_map);
                            }
                        });

                        shutdown();
                        break;

                    case ResCode.STT_NOTI:
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                procResultStt(ResCode.STT_NOTI, result_map);
                            }
                        });
                        break;

                    default:
                        Log.e("AAA", "########## @ WS.onMessage(text) >> resCode=" + ((Double) result_map.get("resCode")).intValue());
                        shutdown();
                        if(mCallback != null) mCallback.onError(mAIEngHandler.this, ResCode.FAIL___INTERNAL_SERVER, "internal server error");
                        break;
                }

            } catch (final Exception e) {
                if(mCallback != null) {
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mCallback.onError(mAIEngHandler.this, ResCode.FAIL___INTERNAL_SERVER, e.getMessage());
                            }
                        });
                }

                shutdown();
            }

        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            super.onMessage(webSocket, bytes);
            Log.e("AAA", "@ WS ............................ onMessage(bin)");
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);
            Log.e("AAA", "@ WS ............................ onClosing()");
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            super.onClosed(webSocket, code, reason);
            Log.e("AAA", "@ WS ............................ onClosed()");
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            super.onFailure(webSocket, t, response);
            /* 정상적인 단절일 경우 바로 종료 */
            if(mIsNormalClosing) return;

            Log.e("AAA", "@ WS ............................ onFailure()");

            shutdown();
            if(mCallback != null) mCallback.onError(mAIEngHandler.this, ResCode.FAIL___INTERNAL_SERVER, "internal server error");
        }
    };

    /* ============================================================================================================== */
    // 결과 처리
    /* ============================================================================================================== */

    void procResultStt(int res_code, Map<String, Object> resultMap) {
        SttResult result = new SttResult();

        result.setSentenceScore(((Double) resultMap.get("sentenceScore")).intValue());

        result.setAnswerText((String) resultMap.get("answerText"));
        result.setResultText((String) resultMap.get("resultText"));
        result.setRecordUrl((String) resultMap.get("recordUrl"));

        if(mCallback != null) {
            if (res_code == ResCode.STT_NOTI) mCallback.onNotiStt(this, result);
            else mCallback.onSuccess(this, result);
        }
    }

    void procResultPron(Map<String, Object> resultMap) {
        final PronResult result = new PronResult();

        result.setTotalScore(((Double) resultMap.get("totalScore")).intValue());
        result.setSpeedScore(((Double) resultMap.get("speedScore")).intValue());
        result.setRhythmScore(((Double) resultMap.get("rhythmScore")).intValue());
        result.setPronScore(((Double) resultMap.get("pronScore")).intValue());
        result.setSegmentalScore(((Double) resultMap.get("segmentalScore")).intValue());
        result.setIntonationScore(((Double) resultMap.get("intonationScore")).intValue());
        result.setWords((List<LinkedTreeMap<String, Object>>) resultMap.get("words"));

        if(mCallback != null) mCallback.onSuccess(this, result);
    }

    void procResultPhonics(Map<String, Object> resultMap) {
        final PhonicsResult result = new PhonicsResult();

        if(mCallback != null) mCallback.onSuccess(this, result);
    }

    /* ============================================================================================================= */
    // 입력 오디오
    /* ============================================================================================================= */

    /* 오디오 입력 시작 */
    void openAudio() {
        try {
            mAudioDispatcher = AudioDispatcherFactory.fromDefaultMicrophone(
                    16000,
                    1024, 0);

            mAudioDispatcher.addAudioProcessor(mAudioCallback);

            AudioProcessor pitchProcessor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
                    16000,
                    1024, mDetector_EPD);
            mAudioDispatcher.addAudioProcessor(pitchProcessor);

            Thread audioThread = new Thread(mAudioDispatcher, "Audio Thread");
            audioThread.start();
        } catch(Exception e) {
            Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /* 오디오 입력 종료 */
    void closeAudio() {
        if(mAudioDispatcher != null) {
            if(!mAudioDispatcher.isStopped()) mAudioDispatcher.stop();
            mAudioDispatcher = null;
        }
    }


    /*
    ** 입력 오디오 콜백
    */
    AudioProcessor mAudioCallback = new AudioProcessor() {
        @Override
        public boolean process(AudioEvent audioEvent) {
            if(mWebSocket != null && mIsStartingAudio == false) {
                mIsStartingAudio = true;
                if(mCallback != null) {
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onStart(mAIEngHandler.this);
                        }
                    });
                }
            }

            try {
                if(mIsStreaming) {
                    // Pitch 콜백과 AudioCallback.process() 콜백의 로그가 출력되지 않으면, Frame 회수만큼
                    // 로직이 실행되지 않는다. (스트리밍은 실제 frame 수만큼 전송됨)
                    Log.e("AAA", "@ WS ..................................... send(audio) = " + audioEvent.getByteBuffer().length);

                    mWebSocket.send(ByteString.of(audioEvent.getByteBuffer()));
                } else {
                    // Pitch 콜백과 AudioCallback.process() 콜백의 로그가 출력되지 않으면, Frame 회수만큼
                    // 로직이 실행되지 않는다. (스트리밍은 실제 frame 수만큼 전송됨)
                    Log.e("AAA", "@ WS ..................................... skip.. audio = " + audioEvent.getByteBuffer().length);
                    mWebSocket.send(ByteString.of(new byte[] {0, 0}));
                }
            } catch (Exception e) {
                Log.e("AAA", "########## @ InputAudio.process() >> Exception >> " + e.getMessage());
                mWebSocket.send(ByteString.of(audioEvent.getByteBuffer()));
            }

            return true;
        }

        @Override
        public void processingFinished() {
            Log.e("AAA", "@ InputAudio .............. processingFinished()");
        }
    };


    /*
    ** 입력 오디오의 피치로 클라이언트에서 EPD 기능을 수행 (좀더 빠른 EPD 검출)
    */
    long mLastPitchingTime = 0;
    PitchDetectionHandler mDetector_EPD = new PitchDetectionHandler() {
        @Override
        public void handlePitch(PitchDetectionResult res, AudioEvent audioEvent) {
            final float pitchInHz = res.getPitch();

            // Pitch 콜백과 AudioCallback.process() 콜백의 로그가 출력되지 않으면, Frame 회수만큼
            // 로직이 실행되지 않는다. (스트리밍은 실제 frame 수만큼 전송됨)
            Log.e("AAA", "@ InputAudio ................. pitch = " + pitchInHz);

            // 스트리밍 전송이 끝났으면 바로 리턴
            if(mIsStreaming == false) return;

            /* EPD 확인 */
            long interval = System.currentTimeMillis() - mLastPitchingTime;
            if(mIsSpeaking == true && pitchInHz <= PITCH___END && interval > EPD_TIMEOUT) {
                Log.e("AAA", "@ InputAudio .......................... Detected EPD !!");

                /* 1바이트 데이타 전송으로 STT 전송의 마지막을 알린다. */
                try {
                    mWebSocket.send(ByteString.of(new byte[] {0}));
                } catch (Exception e1) {}

                mIsStreaming = false;
                return;
            }

            /* 발화 시작은 판단은 둔감하게 100 초과 */
            if(mIsSpeaking == false && pitchInHz <= PITCH___BEGIN) return;

            /* 발화 시작 */
            if(mIsSpeaking == false) mIsSpeaking = true;

            /* 발화 진행중일때 무음은 민감하게 */
            else if(mIsSpeaking == true && pitchInHz < 90) return;

            /* 마지막 발화 시각 기록 */
            mLastPitchingTime = System.currentTimeMillis();

        }
    };



    /* ============================================================================================================== */
    // 콜백
    /* ============================================================================================================== */

    public mAIEngCallback mCallback = null;

    public interface mAIEngCallback {
        public void onStart(mAIEngHandler handler);
        public void onCancel(mAIEngHandler handler);
        public void onNotiStt(mAIEngHandler handler, SttResult result);        /* PRON, PHONICS API 일때, 음성인식 후 호출됨 */
        public void onSuccess(mAIEngHandler handler, SttResult result);        /* STT API 일때, 음성인식 결과 반환 */
        public void onSuccess(mAIEngHandler handler, PronResult result);       /* PRON API 일때, 발음평가 결과 반환 */
        public void onSuccess(mAIEngHandler handler, PhonicsResult result);    /* PHONICS API 일때, 파닉스평가 결과 반환 */
        public void onError(mAIEngHandler handler, int reason, String message);
        public void onProgress(mAIEngHandler handler, float pitch);
    }

    public void setCallback(mAIEngCallback cb) {
        mCallback = cb;
    }

    Handler mHandler = new Handler();
}

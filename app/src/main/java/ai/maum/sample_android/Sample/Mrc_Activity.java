package ai.maum.sample_android.Sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.Gson;

import ai.maum.sample_android.Main_Activity;
import ai.maum.sample_android.Model.MrcRequest;
import ai.maum.sample_android.Model.MrcResponse;
import ai.maum.sample_android.R;
import ai.maum.sample_android.RestAPI.RetroFitConnection;
import ai.maum.sample_android.Utils;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Mrc_Activity extends AppCompatActivity {
    final String TAG = "MRC";

    @BindView(R.id.ScrollView)
    ScrollView mScrollView;

    @BindView(R.id.Edit_Context)
    EditText mEdit_Context;

    @BindView(R.id.Edit_Question)
    EditText mEdit_Question;

    @BindView(R.id.Text_Result)
    TextView mText_Result;

    @BindView(R.id.Text_ResultMessage)
    TextView mText_ResultMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mrc___activity);
        ButterKnife.bind(this);
    }

    /* MRC 실행 */
    @OnClick(R.id.Button_Exec)
    public void OnClick_Exec() {
        /* 파라메타 설정 */
        MrcRequest request = new MrcRequest();
        request.setApiId(Utils.getMetaData(this, "api_id"));
        request.setApiKey(Utils.getMetaData(this, "api_key"));
        request.setLang("kor");
        request.setContext(mEdit_Context.getText().toString());
        request.setQuestion(mEdit_Question.getText().toString());

        /* AI 독해 RestAPI 실행 */
        RetroFitConnection conn = new RetroFitConnection();
        Call<MrcResponse> call = conn.mrc.exec(request);
        call.enqueue(new Callback<MrcResponse>() {
            @Override
            public void onResponse(Call<MrcResponse> call, Response<MrcResponse> response) {
                Gson gson = new Gson();
                if (response.isSuccessful()) {

                    mText_Result.setText( response.body().getAnswer() );
                    mText_ResultMessage.setText( Utils.rebuildJson(gson.toJson(response.body())) );
                    mText_ResultMessage.post(new Runnable() {
                        @Override
                        public void run() {
                            mScrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    });

                } else {
                    // 서버와 연결은 되었으나, 오류 발생
                    Log.d(TAG, "onResponse: fail"); //서버와 연결 실패
                }
            };

            @Override
            public void onFailure(Call<MrcResponse> call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.toString()); //서버와 연결 실패
            }
        });
    }


}

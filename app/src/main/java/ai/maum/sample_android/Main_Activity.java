package ai.maum.sample_android;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import ai.maum.sample_android.Sample.EngEduStt_Activity;
import ai.maum.sample_android.Sample.Mrc_Activity;
import ai.maum.sample_android.Sample.Stt_Activity;
import ai.maum.sample_android.Sample.Tts_Activity;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class Main_Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main___activity);
        ButterKnife.bind(this);

        initPermission();
    }

    /* MRC 샘플 */
    @OnClick(R.id.Button_Mrc)
    public void OnClick_Mrc() {
        Intent it = new Intent(Main_Activity.this, Mrc_Activity.class);
        startActivity(it);
    }

    /* STT 샘플 */
    @OnClick(R.id.Button_Stt)
    public void OnClick_Stt() {
        Intent it = new Intent(Main_Activity.this, Stt_Activity.class);
        startActivity(it);
    }

    /* TTS 샘플 */
    @OnClick(R.id.Button_Tts)
    public void OnClick_Tts() {
        Intent it = new Intent(Main_Activity.this, Tts_Activity.class);
        startActivity(it);
    }

    /* 영어교육 STT 샘플 */
    @OnClick(R.id.Button_EngEduStt)
    public void OnClick_EngEduStt() {
        Intent it = new Intent(Main_Activity.this, EngEduStt_Activity.class);
        startActivity(it);
    }


    /* ======================================================================================================================== */
    // 권한 체크
    /* ======================================================================================================================== */

    void initPermission() {
        /* 권한이 없으면 권한 획득을 위한 Dialog를 연다 */
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for(int xx = 0; xx < grantResults.length; xx++) {
            if(permissions[xx].equals(Manifest.permission.RECORD_AUDIO) == true && grantResults[xx] != 0) {
                Toast.makeText(this, "마이크 권한이 허용되지 않으면\n서비스를 이용할 수 없습니다.", Toast.LENGTH_LONG).show();
            }

            if(permissions[xx].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) == true && grantResults[xx] != 0) {
                Toast.makeText(this, "스토리지 접근 권한이 허용되지 않으면\n서비스를 이용할 수 없습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }
}

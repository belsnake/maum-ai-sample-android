package ai.maum.sample_android.mAIEng;

import lombok.Data;

@Data
public class PhonicsResult {
    int         res_code;

    int         gscore;
    int         pscore;

    String      answer_text;
    String      user_text;
    String      record_url;
}

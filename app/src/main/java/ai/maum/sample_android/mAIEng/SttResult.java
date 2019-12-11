package ai.maum.sample_android.mAIEng;

import lombok.Data;

@Data
public class SttResult {
    int         sentenceScore;

    String      answerText;
    String      resultText;
    String      recordUrl;
}

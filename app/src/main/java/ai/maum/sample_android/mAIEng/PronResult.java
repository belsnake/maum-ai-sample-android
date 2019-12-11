package ai.maum.sample_android.mAIEng;

import com.google.gson.internal.LinkedTreeMap;

import java.util.List;

import lombok.Data;

@Data
public class PronResult {
    int         totalScore;
    int         pronScore;
    int         speedScore;
    int         rhythmScore;
    int         intonationScore;
    int         segmentalScore;
    List<LinkedTreeMap<String, Object>> words;
}

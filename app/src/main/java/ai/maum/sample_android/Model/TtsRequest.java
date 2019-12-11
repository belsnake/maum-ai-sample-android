package ai.maum.sample_android.Model;

import lombok.Data;

@Data
public class TtsRequest {
    private String apiId;
    private String apiKey;
    private String text;
    private String voiceName;
}

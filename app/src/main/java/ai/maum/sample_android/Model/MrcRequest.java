package ai.maum.sample_android.Model;

import lombok.Data;

@Data
public class MrcRequest {
    private String apiId;
    private String apiKey;
    private String lang;
    private String context;
    private String question;
}

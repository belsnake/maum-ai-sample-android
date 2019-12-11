package ai.maum.sample_android.Model;

import lombok.Data;

@Data
public class MrcResponse {
    Message message;
    private String answer;
    private double prob;
    private int startIdx;
    private int endIdx;

    @Data
    class Message {
        private String message;
        private int status;
    }
}

package ai.maum.sample_android.Model;

import lombok.Data;

@Data
public class FaceRecogResponse {
    Message message;
    Result result;

    @Data
    public class Message {
        private String message;
        private int status;
    }

    @Data
    public class Result {
        private String id;
    }
}

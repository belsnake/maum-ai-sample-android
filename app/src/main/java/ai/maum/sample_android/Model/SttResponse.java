package ai.maum.sample_android.Model;

import lombok.Data;

@Data
public class SttResponse {
    private String status;
    private ExtraData message;
    private String data;

    @Data
    class ExtraData {
        private String stt_data;
        private String stt_duration;
    }
}

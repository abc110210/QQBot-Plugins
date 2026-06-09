package xlingran.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PushEnvelope {
    private String id;
    private String event;
    private String timestamp;
    private Boolean test;
    private TestTarget testTarget;
    private SiteInfo site;
    private Map<String, Object> data;

    @Data
    @Builder
    public static class SiteInfo {
        private String title;
        private String url;
    }

    @Data
    @Builder
    public static class TestTarget {
        private String type;
        private String id;
    }
}

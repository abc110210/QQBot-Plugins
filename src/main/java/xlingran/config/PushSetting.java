package xlingran.config;

import java.util.List;
import lombok.Data;

@Data
public class PushSetting {
    public static final String GROUP_PUSH = "push";
    public static final String GROUP_EVENTS = "events";
    public static final String GROUP_TEST = "test";

    private Boolean pushEnabled = true;
    private String pushScriptUrl;
    private String pushAuthToken;
    private Integer pushTimeoutSeconds = 10;

    private List<String> enabledEvents;

    private String testTargetType = "group";
    private String testTargetId;
    private String testTemplate = "post";
}

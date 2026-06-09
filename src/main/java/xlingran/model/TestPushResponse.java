package xlingran.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestPushResponse {
    private boolean ok;
    private String message;
    private String templateType;
    private String mode;
    private String name;
    private String title;
    private String contentPreview;
    private Long upvote;
}

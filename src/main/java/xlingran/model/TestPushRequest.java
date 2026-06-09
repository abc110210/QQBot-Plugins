package xlingran.model;

import lombok.Data;

@Data
public class TestPushRequest {
    private String templateType;
    /** random（默认）或 latest，仅对 moment 有效 */
    private String mode;
}

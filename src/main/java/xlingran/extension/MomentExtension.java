package xlingran.extension;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

@GVK(group = "moment.halo.run", version = "v1alpha1", kind = "Moment",
    plural = "moments", singular = "moment")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MomentExtension extends AbstractExtension {

    public static final String REQUIRE_SYNC_ON_STARTUP_INDEX_NAME = "requireSyncOnStartup";

    private MomentSpec spec;
    private Status status;

    @Data
    public static class MomentSpec {
        private MomentContent content;
        private Instant releaseTime;
        private String visible;
        private String owner;
        private Set<String> tags;
        private Boolean approved;
        private Instant approvedTime;
    }

    @Data
    public static class Status {
        private long observedVersion;
        private String permalink;
    }

    @Data
    public static class MomentContent {
        private String raw;
        private String html;

        @ArraySchema(schema = @Schema(description = "Media item"))
        private List<MomentMedia> medium;
    }

    @Data
    public static class MomentMedia {
        private String type;
        private String url;
        private String originType;
    }
}

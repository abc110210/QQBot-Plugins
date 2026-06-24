package xlingran;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import xlingran.service.CommentReplyPushTracker;
import xlingran.service.MomentPushTracker;

@Slf4j
@Component
public class Shan extends BasePlugin {

    private final MomentPushTracker momentPushTracker;
    private final CommentReplyPushTracker commentReplyPushTracker;

    public Shan(PluginContext pluginContext, MomentPushTracker momentPushTracker,
                CommentReplyPushTracker commentReplyPushTracker) {
        super(pluginContext);
        this.momentPushTracker = momentPushTracker;
        this.commentReplyPushTracker = commentReplyPushTracker;
    }

    @Override
    public void start() {
        momentPushTracker.loadExistingMoments();
        commentReplyPushTracker.loadExisting();
        log.info("xlingran-shan 插件已启动");
    }

    @Override
    public void stop() {
        log.info("xlingran-shan 插件已停止");
    }
}

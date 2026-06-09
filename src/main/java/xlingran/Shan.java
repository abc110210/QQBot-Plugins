package xlingran;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import xlingran.service.MomentPushTracker;

@Slf4j
@Component
public class Shan extends BasePlugin {

    private final MomentPushTracker momentPushTracker;

    public Shan(PluginContext pluginContext, MomentPushTracker momentPushTracker) {
        super(pluginContext);
        this.momentPushTracker = momentPushTracker;
    }

    @Override
    public void start() {
        momentPushTracker.loadExistingMoments();
        log.info("xlingran-shan 插件已启动");
    }

    @Override
    public void stop() {
        log.info("xlingran-shan 插件已停止");
    }
}

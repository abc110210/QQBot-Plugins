package xlingran.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import run.halo.app.plugin.PluginConfigUpdatedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushConfigListener {

    @EventListener
    public void onConfigUpdated(PluginConfigUpdatedEvent event) {
        if (event.getNewConfig().containsKey(PushSetting.GROUP_PUSH)
            || event.getNewConfig().containsKey(PushSetting.GROUP_TEST)) {
            log.info("xlingran-shan 插件配置已更新");
        }
    }
}

package xlingran.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import xlingran.config.PushSetting;

@Service
@RequiredArgsConstructor
public class PushSettingService {

    private final ReactiveSettingFetcher settingFetcher;

    public Mono<PushSetting> fetch() {
        return Mono.zip(
            settingFetcher.fetch(PushSetting.GROUP_PUSH, PushSetting.class),
            settingFetcher.fetch(PushSetting.GROUP_EVENTS, PushSetting.class),
            settingFetcher.fetch(PushSetting.GROUP_TEST, PushSetting.class)
        ).map(tuple -> {
            var push = tuple.getT1();
            var events = tuple.getT2();
            var test = tuple.getT3();
            var merged = new PushSetting();
            merged.setPushEnabled(push.getPushEnabled());
            merged.setPushScriptUrl(push.getPushScriptUrl());
            merged.setPushAuthToken(push.getPushAuthToken());
            merged.setPushTimeoutSeconds(push.getPushTimeoutSeconds());
            merged.setEnabledEvents(events.getEnabledEvents());
            merged.setTestTargetType(test.getTestTargetType());
            merged.setTestTargetId(test.getTestTargetId());
            merged.setTestTemplate(test.getTestTemplate());
            return merged;
        });
    }

    public boolean isEventEnabled(PushSetting setting, String eventKey) {
        var enabled = setting.getEnabledEvents();
        if (enabled == null || enabled.isEmpty()) {
            return true;
        }
        return enabled.contains(eventKey);
    }
}

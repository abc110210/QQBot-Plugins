package xlingran.service;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xlingran.model.PushEnvelope;

@Service
@RequiredArgsConstructor
public class TestPushService {

    private final PushSettingService pushSettingService;
    private final RemotePushService remotePushService;
    private final TestContentSampler testContentSampler;
    private final PostPayloadBuilder postPayloadBuilder;
    private final MomentPayloadBuilder momentPayloadBuilder;

    public Mono<Void> sendTest(String templateTypeOverride) {
        return pushSettingService.fetch().flatMap(setting -> {
            var template = StringUtils.hasText(templateTypeOverride)
                ? templateTypeOverride
                : setting.getTestTemplate();
            if (!StringUtils.hasText(template)) {
                template = "post";
            }
            if (!StringUtils.hasText(setting.getTestTargetId())) {
                return Mono.error(new IllegalStateException("请先在插件设置中填写测试群号 / QQ 号"));
            }

            var isMoment = "moment".equalsIgnoreCase(template);
            var event = isMoment ? "test.moment" : "test.post";

            return Mono.fromCallable(() -> buildTestData(isMoment))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(data -> remotePushService.currentSite().flatMap(site -> {
                    var envelope = PushEnvelope.builder()
                        .id(UUID.randomUUID().toString())
                        .event(event)
                        .timestamp(java.time.Instant.now().toString())
                        .test(true)
                        .testTarget(PushEnvelope.TestTarget.builder()
                            .type(StringUtils.hasText(setting.getTestTargetType())
                                ? setting.getTestTargetType() : "group")
                            .id(setting.getTestTargetId())
                            .build())
                        .site(site)
                        .data(data)
                        .build();
                    return remotePushService.push(envelope);
                }));
        });
    }

    private Map<String, Object> buildTestData(boolean isMoment) {
        if (isMoment) {
            return momentPayloadBuilder.build(
                testContentSampler.randomApprovedMoment(), "test");
        }
        return postPayloadBuilder.build(testContentSampler.randomPublishedPost(), "test");
    }
}

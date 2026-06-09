package xlingran.service;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xlingran.extension.MomentExtension;
import xlingran.model.PushEnvelope;
import xlingran.model.TestPushResponse;

@Service
@RequiredArgsConstructor
public class TestPushService {

    private final PushSettingService pushSettingService;
    private final RemotePushService remotePushService;
    private final TestContentSampler testContentSampler;
    private final PostPayloadBuilder postPayloadBuilder;
    private final MomentPayloadBuilder momentPayloadBuilder;

    public Mono<TestPushResponse> sendTest(String templateTypeOverride, String mode) {
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
            var resolvedMode = resolveMode(mode, isMoment);

            return Mono.fromCallable(() -> buildTestData(isMoment, resolvedMode))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(result -> remotePushService.currentSite().flatMap(site -> {
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
                        .data(result.data())
                        .build();
                    return remotePushService.push(envelope).thenReturn(toResponse(isMoment, resolvedMode, result));
                }));
        });
    }

    private static String resolveMode(String mode, boolean isMoment) {
        if (!isMoment) {
            return "random";
        }
        if ("latest".equalsIgnoreCase(mode)) {
            return "latest";
        }
        return "random";
    }

    private TestBuildResult buildTestData(boolean isMoment, String mode) {
        if (isMoment) {
            MomentExtension moment = "latest".equals(mode)
                ? testContentSampler.latestApprovedMoment()
                : testContentSampler.randomApprovedMoment();
            var data = momentPayloadBuilder.build(moment, "test");
            return new TestBuildResult(data, moment.getMetadata().getName());
        }
        var post = testContentSampler.randomPublishedPost();
        var data = postPayloadBuilder.build(post, "test");
        return new TestBuildResult(data, post.getMetadata().getName());
    }

    private TestPushResponse toResponse(boolean isMoment, String mode, TestBuildResult result) {
        var data = result.data();
        var builder = TestPushResponse.builder()
            .ok(true)
            .message("测试推送已发送")
            .templateType(isMoment ? "moment" : "post")
            .mode(mode)
            .name(result.name());
        if (isMoment) {
            builder.contentPreview(stringValue(data.get("contentPreview")))
                .upvote(longValue(data.get("upvote")));
        } else {
            builder.title(stringValue(data.get("title")))
                .contentPreview(stringValue(data.get("excerpt")));
        }
        return builder.build();
    }

    private static String stringValue(Object value) {
        return value != null ? value.toString() : "";
    }

    private static Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private record TestBuildResult(Map<String, Object> data, String name) {
    }
}

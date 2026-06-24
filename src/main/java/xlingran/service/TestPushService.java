package xlingran.service;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.halo.app.core.extension.content.Comment;
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
    private final ContentEventBridge contentEventBridge;

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

            var kind = resolveKind(template);
            var event = switch (kind) {
                case MOMENT -> "test.moment";
                case COMMENT -> "test.comment";
                default -> "test.post";
            };
            var resolvedMode = resolveMode(mode, kind);

            return Mono.fromCallable(() -> buildTestData(kind, resolvedMode))
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
                    return remotePushService.push(envelope).thenReturn(toResponse(kind, resolvedMode, result));
                }));
        });
    }

    enum Kind { POST, MOMENT, COMMENT }

    private static Kind resolveKind(String template) {
        if ("moment".equalsIgnoreCase(template)) {
            return Kind.MOMENT;
        }
        if ("comment".equalsIgnoreCase(template) || "reply".equalsIgnoreCase(template)) {
            return Kind.COMMENT;
        }
        return Kind.POST;
    }

    private static String resolveMode(String mode, Kind kind) {
        if (kind != Kind.MOMENT) {
            return "random";
        }
        return "latest".equalsIgnoreCase(mode) ? "latest" : "random";
    }

    private TestBuildResult buildTestData(Kind kind, String mode) {
        if (kind == Kind.MOMENT) {
            MomentExtension moment = "latest".equals(mode)
                ? testContentSampler.latestApprovedMoment()
                : testContentSampler.randomApprovedMoment();
            var data = momentPayloadBuilder.build(moment, "test");
            return new TestBuildResult(data, moment.getMetadata().getName());
        }
        if (kind == Kind.COMMENT) {
            Comment comment = testContentSampler.randomApprovedComment();
            var data = contentEventBridge.buildCommentData(comment, "comment", "created");
            return new TestBuildResult(data, comment.getMetadata().getName());
        }
        var post = testContentSampler.randomPublishedPost();
        var data = postPayloadBuilder.build(post, "test");
        return new TestBuildResult(data, post.getMetadata().getName());
    }

    private TestPushResponse toResponse(Kind kind, String mode, TestBuildResult result) {
        var data = result.data();
        var builder = TestPushResponse.builder()
            .ok(true)
            .message("测试推送已发送")
            .templateType(kind == Kind.MOMENT ? "moment" : kind == Kind.COMMENT ? "comment" : "post")
            .mode(mode)
            .name(result.name());
        if (kind == Kind.MOMENT) {
            builder.contentPreview(stringValue(data.get("contentPreview")))
                .upvote(longValue(data.get("upvote")));
        } else if (kind == Kind.COMMENT) {
            builder.contentPreview(stringValue(data.get("contentPreview")));
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

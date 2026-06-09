package xlingran.service;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.content.Post;
import run.halo.app.infra.SystemInfoGetter;
import xlingran.config.PushSetting;
import xlingran.model.PushEnvelope;

@Slf4j
@Service
@RequiredArgsConstructor
public class RemotePushService {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(DISPLAY_ZONE);

    private final WebClient.Builder webClientBuilder;
    private final PushSettingService pushSettingService;
    private final SystemInfoGetter systemInfoGetter;

    public Mono<Void> push(PushEnvelope envelope) {
        return pushSettingService.fetch()
            .flatMap(setting -> {
                if (!Boolean.TRUE.equals(setting.getPushEnabled())) {
                    log.warn("推送已关闭，跳过 event={}", envelope.getEvent());
                    return Mono.empty();
                }
                if (!StringUtils.hasText(setting.getPushScriptUrl())) {
                    log.warn("pushScriptUrl 未配置，跳过推送 event={}", envelope.getEvent());
                    return Mono.empty();
                }
                if (!pushSettingService.isEventEnabled(setting, envelope.getEvent())
                    && !Boolean.TRUE.equals(envelope.getTest())) {
                    log.warn("事件未启用，跳过 event={}", envelope.getEvent());
                    return Mono.empty();
                }
                return send(setting, envelope);
            });
    }

    private Mono<Void> send(PushSetting setting, PushEnvelope envelope) {
        var timeout = Duration.ofSeconds(
            setting.getPushTimeoutSeconds() != null ? setting.getPushTimeoutSeconds() : 10
        );
        var client = webClientBuilder.build();
        return doSend(client, setting, envelope, timeout)
            .onErrorResume(err -> {
                log.warn("远程推送失败，重试一次 event={} id={}", envelope.getEvent(), envelope.getId(), err);
                return doSend(client, setting, envelope, timeout);
            })
            .then()
            .onErrorResume(err -> {
                log.error("远程推送最终失败 event={} id={}", envelope.getEvent(), envelope.getId(), err);
                return Mono.empty();
            });
    }

    private Mono<String> doSend(
        org.springframework.web.reactive.function.client.WebClient client,
        PushSetting setting,
        PushEnvelope envelope,
        Duration timeout
    ) {
        var spec = client.post()
            .uri(URI.create(setting.getPushScriptUrl()))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(envelope);
        if (StringUtils.hasText(setting.getPushAuthToken())) {
            spec = spec.header("Authorization", "Bearer " + setting.getPushAuthToken());
        }
        return spec.retrieve()
            .bodyToMono(String.class)
            .timeout(timeout)
            .doOnSuccess(body -> log.info("远程推送成功 event={} id={}", envelope.getEvent(), envelope.getId()));
    }

    public Mono<PushEnvelope.SiteInfo> currentSite() {
        return systemInfoGetter.get()
            .map(info -> PushEnvelope.SiteInfo.builder()
                .title(info.getTitle() != null ? info.getTitle() : "")
                .url(info.getUrl() != null ? info.getUrl().toString() : "")
                .build())
            .defaultIfEmpty(PushEnvelope.SiteInfo.builder().title("").url("").build());
    }

    public String formatDate(Instant instant) {
        if (instant == null) {
            return DATE_FMT.format(Instant.now());
        }
        return DATE_FMT.format(instant);
    }

    public String formatPostDate(Post post) {
        var labels = post.getMetadata().getLabels();
        if (labels != null) {
            var year = labels.get(Post.ARCHIVE_YEAR_LABEL);
            var month = labels.get(Post.ARCHIVE_MONTH_LABEL);
            var day = labels.get(Post.ARCHIVE_DAY_LABEL);
            if (StringUtils.hasText(year) && StringUtils.hasText(month) && StringUtils.hasText(day)) {
                return String.format("%s-%s-%s", year, pad2(month), pad2(day));
            }
        }
        var instant = post.getSpec().getPublishTime() != null
            ? post.getSpec().getPublishTime()
            : post.getMetadata().getCreationTimestamp();
        return formatDate(instant);
    }

    private static String pad2(String value) {
        return value.length() == 1 ? "0" + value : value;
    }

    public Mono<PushEnvelope> buildEnvelope(String event, Map<String, Object> data) {
        return currentSite().map(site -> PushEnvelope.builder()
            .id(UUID.randomUUID().toString())
            .event(event)
            .timestamp(Instant.now().toString())
            .site(site)
            .data(data)
            .build());
    }
}

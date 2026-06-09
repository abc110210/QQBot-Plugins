package xlingran.service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.infra.ExternalUrlSupplier;
import xlingran.extension.MomentExtension;
import xlingran.util.PlainTextUtils;

@Service
@RequiredArgsConstructor
public class MomentPayloadBuilder {

    private static final Pattern IMG_SRC_PATTERN =
        Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    private final ExternalLinkProcessor externalLinkProcessor;
    private final ExternalUrlSupplier externalUrlSupplier;
    private final UserDisplayService userDisplayService;
    private final RemotePushService remotePushService;
    private final MomentStatsService momentStatsService;

    public Map<String, Object> build(MomentExtension moment, String action) {
        var data = new HashMap<String, Object>();
        var name = moment.getMetadata().getName();
        data.put("category", "moment");
        data.put("action", action);
        data.put("name", name);
        data.put("contentPreview", preview(moment));
        data.put("releaseTime", moment.getSpec().getReleaseTime());
        data.put("visible", moment.getSpec().getVisible());
        data.put("permalink", resolvePermalink(moment));
        data.put("firstImage", firstImage(moment));
        data.put("owner", moment.getSpec().getOwner());
        data.put("ownerDisplayName", userDisplayService.displayName(moment.getSpec().getOwner()));
        data.put("ownerAvatar", userDisplayService.avatar(moment.getSpec().getOwner()));
        data.put("publishTime", remotePushService.formatDate(
            moment.getSpec().getReleaseTime() != null
                ? moment.getSpec().getReleaseTime()
                : moment.getMetadata().getCreationTimestamp()
        ));

        var stats = momentStatsService.statsFor(name);
        data.put("upvote", stats.upvote());
        data.put("totalComment", stats.totalComment());
        data.put("approvedComment", stats.approvedComment());
        return data;
    }

    private String preview(MomentExtension moment) {
        if (moment.getSpec().getContent() == null) {
            return "";
        }
        var content = moment.getSpec().getContent();
        var fromRaw = PlainTextUtils.stripHtml(content.getRaw());
        if (StringUtils.hasText(fromRaw)) {
            return PlainTextUtils.truncate(fromRaw, 200);
        }
        var fromHtml = PlainTextUtils.stripHtml(content.getHtml());
        if (StringUtils.hasText(fromHtml)) {
            return PlainTextUtils.truncate(fromHtml, 200);
        }
        return "";
    }

    private String firstImage(MomentExtension moment) {
        if (moment.getSpec().getContent() == null) {
            return "";
        }
        var content = moment.getSpec().getContent();
        if (content.getMedium() != null) {
            for (var media : content.getMedium()) {
                if (media == null || !StringUtils.hasText(media.getUrl())) {
                    continue;
                }
                if (isImageMedia(media)) {
                    return resolveUrl(media.getUrl());
                }
            }
        }
        return firstImageFromHtml(content.getHtml());
    }

    private boolean isImageMedia(MomentExtension.MomentMedia media) {
        var type = media.getType();
        if (!StringUtils.hasText(type) || "PHOTO".equalsIgnoreCase(type)
            || type.toLowerCase().contains("image")) {
            return true;
        }
        var originType = media.getOriginType();
        if (StringUtils.hasText(originType) && originType.toLowerCase().startsWith("image/")) {
            return true;
        }
        return hasImageExtension(media.getUrl());
    }

    private boolean hasImageExtension(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        var lower = url.toLowerCase();
        var queryIdx = lower.indexOf('?');
        if (queryIdx >= 0) {
            lower = lower.substring(0, queryIdx);
        }
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
            || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp")
            || lower.endsWith(".svg");
    }

    private String firstImageFromHtml(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        Matcher matcher = IMG_SRC_PATTERN.matcher(html);
        if (matcher.find()) {
            return resolveUrl(matcher.group(1));
        }
        return "";
    }

    private String resolveUrl(String url) {
        return externalLinkProcessor.processLink(url);
    }

    private String resolvePermalink(MomentExtension moment) {
        if (moment.getStatus() != null && StringUtils.hasText(moment.getStatus().getPermalink())) {
            return externalLinkProcessor.processLink(moment.getStatus().getPermalink());
        }
        var base = externalUrlSupplier.get().toString();
        return base.endsWith("/") ? base + "moments" : base + "/moments";
    }
}

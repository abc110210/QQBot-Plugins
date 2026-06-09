package xlingran.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import run.halo.app.core.extension.attachment.Attachment;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.infra.ExternalLinkProcessor;

@Service
@RequiredArgsConstructor
public class AttachmentUrlResolver {

    private final ExtensionClient client;
    private final ExternalLinkProcessor externalLinkProcessor;

    public String resolve(String urlOrPath) {
        if (!StringUtils.hasText(urlOrPath)) {
            return "";
        }
        var linked = externalLinkProcessor.processLink(urlOrPath);
        if (StringUtils.hasText(linked)) {
            return linked;
        }
        return resolveByAttachmentName(extractAttachmentName(urlOrPath));
    }

    public String resolveByAttachmentName(String attachmentName) {
        if (!StringUtils.hasText(attachmentName)) {
            return "";
        }
        return client.fetch(Attachment.class, attachmentName)
            .map(this::permalinkFromAttachment)
            .filter(StringUtils::hasText)
            .map(externalLinkProcessor::processLink)
            .orElse("");
    }

    private String permalinkFromAttachment(Attachment attachment) {
        if (attachment.getStatus() != null && StringUtils.hasText(attachment.getStatus().getPermalink())) {
            return attachment.getStatus().getPermalink();
        }
        return "";
    }

    static String extractAttachmentName(String urlOrPath) {
        if (!StringUtils.hasText(urlOrPath)) {
            return "";
        }
        var path = urlOrPath.trim();
        var slash = path.lastIndexOf('/');
        if (slash >= 0) {
            path = path.substring(slash + 1);
        }
        var query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        var dot = path.lastIndexOf('.');
        if (dot > 0) {
            path = path.substring(0, dot);
        }
        return path;
    }
}

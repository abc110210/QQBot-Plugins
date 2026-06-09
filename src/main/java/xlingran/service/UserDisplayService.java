package xlingran.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import run.halo.app.core.extension.User;
import run.halo.app.core.extension.content.Comment;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.infra.ExternalLinkProcessor;

@Service
@RequiredArgsConstructor
public class UserDisplayService {

    private final ExtensionClient client;
    private final ExternalLinkProcessor externalLinkProcessor;

    public String displayName(String username) {
        if (!StringUtils.hasText(username)) {
            return "";
        }
        return client.fetch(User.class, username)
            .map(u -> {
                if (u.getSpec() != null && StringUtils.hasText(u.getSpec().getDisplayName())) {
                    return u.getSpec().getDisplayName();
                }
                return username;
            })
            .orElse(username);
    }

    public String displayName(Comment.CommentOwner owner) {
        if (owner == null) {
            return "";
        }
        if (StringUtils.hasText(owner.getDisplayName())) {
            return owner.getDisplayName();
        }
        if ("User".equals(owner.getKind())) {
            return displayName(owner.getName());
        }
        return owner.getName() != null ? owner.getName() : "";
    }

    public String avatar(String username) {
        if (!StringUtils.hasText(username)) {
            return "";
        }
        return client.fetch(User.class, username)
            .map(u -> u.getSpec() != null ? u.getSpec().getAvatar() : "")
            .filter(StringUtils::hasText)
            .map(externalLinkProcessor::processLink)
            .orElse("");
    }
}

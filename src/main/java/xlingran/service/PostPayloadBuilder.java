package xlingran.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import run.halo.app.core.extension.content.Category;
import run.halo.app.core.extension.content.Post;
import run.halo.app.core.extension.content.Tag;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.infra.ExternalLinkProcessor;
import xlingran.model.LabelItem;

@Service
@RequiredArgsConstructor
public class PostPayloadBuilder {

    private final ExtensionClient client;
    private final ExternalLinkProcessor externalLinkProcessor;
    private final UserDisplayService userDisplayService;
    private final RemotePushService remotePushService;

    public Map<String, Object> build(Post post, String action) {
        var data = new HashMap<String, Object>();
        data.put("category", "post");
        data.put("action", action);
        data.put("name", post.getMetadata().getName());
        data.put("title", post.getSpec().getTitle());
        data.put("permalink", resolvePermalink(post));
        data.put("cover", resolveCover(post.getSpec().getCover()));
        data.put("excerpt", resolveExcerpt(post));
        data.put("owner", post.getSpec().getOwner());
        data.put("ownerDisplayName", userDisplayService.displayName(post.getSpec().getOwner()));
        data.put("ownerAvatar", userDisplayService.avatar(post.getSpec().getOwner()));
        data.put("publishTime", remotePushService.formatPostDate(post));
        data.put("categories", resolveCategories(post));
        data.put("tags", resolveTags(post));
        return data;
    }

    private String resolvePermalink(Post post) {
        if (post.getStatus() != null && StringUtils.hasText(post.getStatus().getPermalink())) {
            return externalLinkProcessor.processLink(post.getStatus().getPermalink());
        }
        return externalLinkProcessor.processLink("/");
    }

    private String resolveCover(String cover) {
        if (!StringUtils.hasText(cover)) {
            return "";
        }
        return externalLinkProcessor.processLink(cover);
    }

    private String resolveExcerpt(Post post) {
        if (post.getStatus() != null && StringUtils.hasText(post.getStatus().getExcerpt())) {
            return post.getStatus().getExcerpt();
        }
        return "";
    }

    private List<LabelItem> resolveCategories(Post post) {
        var names = post.getSpec().getCategories();
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        return names.stream()
            .map(name -> client.fetch(Category.class, name).orElse(null))
            .filter(c -> c != null)
            .map(c -> LabelItem.builder()
                .name(c.getMetadata().getName())
                .displayName(c.getSpec().getDisplayName())
                .permalink(c.getStatus() != null && StringUtils.hasText(c.getStatus().getPermalink())
                    ? externalLinkProcessor.processLink(c.getStatus().getPermalink())
                    : null)
                .build())
            .collect(Collectors.toList());
    }

    private List<LabelItem> resolveTags(Post post) {
        var names = post.getSpec().getTags();
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        return names.stream()
            .map(name -> client.fetch(Tag.class, name).orElse(null))
            .filter(t -> t != null)
            .map(t -> LabelItem.builder()
                .name(t.getMetadata().getName())
                .displayName(t.getSpec().getDisplayName())
                .permalink(t.getStatus() != null && StringUtils.hasText(t.getStatus().getPermalink())
                    ? externalLinkProcessor.processLink(t.getStatus().getPermalink())
                    : null)
                .build())
            .collect(Collectors.toList());
    }
}

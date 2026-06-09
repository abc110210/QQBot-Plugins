package xlingran.service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import run.halo.app.core.extension.content.Post;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ListOptions;
import static run.halo.app.extension.index.query.Queries.and;
import static run.halo.app.extension.index.query.Queries.equal;
import xlingran.extension.MomentExtension;

@Service
@RequiredArgsConstructor
public class TestContentSampler {

    private final ExtensionClient client;
    private final MomentFetcher momentFetcher;

    public Post randomPublishedPost() {
        var options = ListOptions.builder()
            .labelSelector()
            .eq(Post.PUBLISHED_LABEL, Boolean.TRUE.toString())
            .end()
            .build();
        var names = client.listAllNames(Post.class, options, Sort.unsorted());
        if (names.isEmpty()) {
            throw new IllegalStateException("没有已发布的文章可用于测试");
        }
        var name = pickRandom(names);
        return client.fetch(Post.class, name)
            .orElseThrow(() -> new IllegalStateException("无法加载文章: " + name));
    }

    public MomentExtension randomApprovedMoment() {
        var options = ListOptions.builder()
            .fieldQuery(and(
                equal("spec.approved", true),
                equal("spec.visible", "PUBLIC")
            ))
            .build();
        var names = client.listAllNames(MomentExtension.class, options, Sort.unsorted());
        if (names.isEmpty()) {
            throw new IllegalStateException("没有已审核的瞬间可用于测试");
        }
        var name = pickRandom(names);
        return momentFetcher.fetch(name)
            .orElseThrow(() -> new IllegalStateException("无法加载瞬间: " + name));
    }

    private static String pickRandom(List<String> names) {
        return names.get(ThreadLocalRandom.current().nextInt(names.size()));
    }
}

package xlingran.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import run.halo.app.core.extension.content.Post;
import run.halo.app.event.post.PostDeletedEvent;
import run.halo.app.event.post.PostPublishedEvent;
import run.halo.app.event.post.PostUnpublishedEvent;
import run.halo.app.event.post.PostUpdatedEvent;
import run.halo.app.event.post.PostVisibleChangedEvent;
import run.halo.app.extension.ExtensionClient;
import xlingran.service.ContentEventBridge;

@Component
@RequiredArgsConstructor
public class PostEventListener {

    private final ExtensionClient client;
    private final ContentEventBridge contentEventBridge;

    @Async
    @EventListener
    public void onPostPublished(PostPublishedEvent event) {
        fetchPost(event.getName()).ifPresent(post -> contentEventBridge.pushPost(post, "published"));
    }

    @Async
    @EventListener
    public void onPostUpdated(PostUpdatedEvent event) {
        fetchPost(event.getName()).ifPresent(post -> contentEventBridge.pushPost(post, "updated"));
    }

    @Async
    @EventListener
    public void onPostDeleted(PostDeletedEvent event) {
        fetchPost(event.getName()).ifPresent(post -> contentEventBridge.pushPost(post, "deleted"));
    }

    @Async
    @EventListener
    public void onPostUnpublished(PostUnpublishedEvent event) {
        fetchPost(event.getName()).ifPresent(post -> contentEventBridge.pushPost(post, "unpublished"));
    }

    @Async
    @EventListener
    public void onPostVisibleChanged(PostVisibleChangedEvent event) {
        fetchPost(event.getName()).ifPresent(post -> contentEventBridge.pushPost(post, "visibility_changed"));
    }

    private java.util.Optional<Post> fetchPost(String name) {
        return client.fetch(Post.class, name);
    }
}

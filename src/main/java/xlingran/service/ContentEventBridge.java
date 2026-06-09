package xlingran.service;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import run.halo.app.core.extension.content.Comment;
import run.halo.app.core.extension.content.Post;
import run.halo.app.core.extension.content.Reply;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.Ref;
import xlingran.extension.MomentExtension;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentEventBridge {

    private final RemotePushService remotePushService;
    private final PostPayloadBuilder postPayloadBuilder;
    private final MomentPayloadBuilder momentPayloadBuilder;
    private final ExtensionClient client;
    private final MomentFetcher momentFetcher;
    private final UserDisplayService userDisplayService;

    public void pushPost(Post post, String action) {
        var event = "post." + action;
        var data = postPayloadBuilder.build(post, action);
        remotePushService.buildEnvelope(event, data)
            .flatMap(remotePushService::push)
            .subscribe();
    }

    public void pushMoment(MomentExtension moment, String action) {
        var event = "moment." + action;
        var name = moment.getMetadata().getName();
        log.debug("Push moment event={} name={} owner={}", event, name,
            moment.getSpec() != null ? moment.getSpec().getOwner() : "");
        var data = momentPayloadBuilder.build(moment, action);
        remotePushService.buildEnvelope(event, data)
            .flatMap(remotePushService::push)
            .subscribe();
    }

    public void pushComment(Comment comment) {
        var data = buildCommentData(comment, "comment", "created");
        remotePushService.buildEnvelope("comment.created", data)
            .flatMap(remotePushService::push)
            .subscribe();
    }

    public void pushReply(Reply reply) {
        var data = new HashMap<String, Object>();
        data.put("category", "reply");
        data.put("action", "created");
        data.put("name", reply.getMetadata().getName());
        data.put("contentPreview", truncate(reply.getSpec().getContent()));
        data.put("owner", ownerKey(reply.getSpec().getOwner()));
        data.put("ownerDisplayName", userDisplayService.displayName(reply.getSpec().getOwner()));
        data.put("commentName", reply.getSpec().getCommentName());
        remotePushService.buildEnvelope("reply.created", data)
            .flatMap(remotePushService::push)
            .subscribe();
    }

    private Map<String, Object> buildCommentData(Comment comment, String category, String action) {
        var data = new HashMap<String, Object>();
        data.put("category", category);
        data.put("action", action);
        data.put("name", comment.getMetadata().getName());
        data.put("contentPreview", truncate(comment.getSpec().getContent()));
        data.put("owner", ownerKey(comment.getSpec().getOwner()));
        data.put("ownerDisplayName", userDisplayService.displayName(comment.getSpec().getOwner()));
        var subjectRef = comment.getSpec().getSubjectRef();
        if (subjectRef != null) {
            data.put("subjectKind", subjectRef.getKind());
            data.put("subjectName", subjectRef.getName());
            data.put("subjectTitle", resolveSubjectTitle(subjectRef));
        }
        return data;
    }

    private String resolveSubjectTitle(Ref subjectRef) {
        if (subjectRef == null) {
            return "";
        }
        if ("Post".equals(subjectRef.getKind()) && "content.halo.run".equals(subjectRef.getGroup())) {
            return client.fetch(Post.class, subjectRef.getName())
                .map(p -> p.getSpec().getTitle())
                .orElse("");
        }
        if ("Moment".equals(subjectRef.getKind()) && "moment.halo.run".equals(subjectRef.getGroup())) {
            return momentFetcher.fetch(subjectRef.getName())
                .map(m -> momentPayloadBuilder.build(m, "created").getOrDefault("contentPreview", "").toString())
                .orElse("");
        }
        return "";
    }

    private String ownerKey(Comment.CommentOwner owner) {
        if (owner == null) {
            return "";
        }
        return Comment.CommentOwner.ownerIdentity(owner.getKind(), owner.getName());
    }

    private String truncate(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.length() > 200 ? text.substring(0, 200) + "…" : text;
    }
}

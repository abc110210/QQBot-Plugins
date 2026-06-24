package xlingran.reconciler;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import run.halo.app.core.extension.content.Comment;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import xlingran.service.CommentReplyPushTracker;
import xlingran.service.ContentEventBridge;

@Component
@RequiredArgsConstructor
public class CommentReconciler implements Reconciler<Reconciler.Request> {

    private static final String FINALIZER = "xlingran-shan/comment-push";

    private final ExtensionClient client;
    private final ContentEventBridge contentEventBridge;
    private final CommentReplyPushTracker commentReplyPushTracker;

    @Override
    public Result reconcile(Request request) {
        client.fetch(Comment.class, request.name()).ifPresent(comment -> {
            if (ExtensionUtil.isDeleted(comment)) {
                if (ExtensionUtil.removeFinalizers(comment.getMetadata(), Set.of(FINALIZER))) {
                    client.update(comment);
                }
                commentReplyPushTracker.forgetComment(request.name());
                return;
            }

            if (ExtensionUtil.addFinalizers(comment.getMetadata(), Set.of(FINALIZER))) {
                client.update(comment);
            }

            // 仅推送启动快照之后新增的评论，历史评论不重推
            if (commentReplyPushTracker.shouldPushComment(request.name())) {
                contentEventBridge.pushComment(comment);
            }
        });
        return new Result(false, null);
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return builder.extension(new Comment()).build();
    }
}

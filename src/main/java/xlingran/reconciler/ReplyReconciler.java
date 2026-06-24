package xlingran.reconciler;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import run.halo.app.core.extension.content.Reply;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import xlingran.service.CommentReplyPushTracker;
import xlingran.service.ContentEventBridge;

@Component
@RequiredArgsConstructor
public class ReplyReconciler implements Reconciler<Reconciler.Request> {

    private static final String FINALIZER = "xlingran-shan/reply-push";

    private final ExtensionClient client;
    private final ContentEventBridge contentEventBridge;
    private final CommentReplyPushTracker commentReplyPushTracker;

    @Override
    public Result reconcile(Request request) {
        client.fetch(Reply.class, request.name()).ifPresent(reply -> {
            if (ExtensionUtil.isDeleted(reply)) {
                if (ExtensionUtil.removeFinalizers(reply.getMetadata(), Set.of(FINALIZER))) {
                    client.update(reply);
                }
                commentReplyPushTracker.forgetReply(request.name());
                return;
            }

            if (ExtensionUtil.addFinalizers(reply.getMetadata(), Set.of(FINALIZER))) {
                client.update(reply);
            }

            // 仅推送启动快照之后新增的回复，历史回复不重推
            if (commentReplyPushTracker.shouldPushReply(request.name())) {
                contentEventBridge.pushReply(reply);
            }
        });
        return new Result(false, null);
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return builder.extension(new Reply()).build();
    }
}

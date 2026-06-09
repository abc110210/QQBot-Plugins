package xlingran.reconciler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import run.halo.app.core.extension.content.Reply;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import xlingran.service.ContentEventBridge;

@Component
@RequiredArgsConstructor
public class ReplyReconciler implements Reconciler<Reconciler.Request> {

    private static final String FINALIZER = "xlingran-shan/reply-push";

    private final ExtensionClient client;
    private final ContentEventBridge contentEventBridge;
    private final Set<String> pushedCreates = ConcurrentHashMap.newKeySet();

    @Override
    public Result reconcile(Request request) {
        client.fetch(Reply.class, request.name()).ifPresent(reply -> {
            if (ExtensionUtil.isDeleted(reply)) {
                if (ExtensionUtil.removeFinalizers(reply.getMetadata(), Set.of(FINALIZER))) {
                    client.update(reply);
                }
                pushedCreates.remove(request.name());
                return;
            }

            if (ExtensionUtil.addFinalizers(reply.getMetadata(), Set.of(FINALIZER))) {
                client.update(reply);
            }

            if (pushedCreates.add(request.name())) {
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

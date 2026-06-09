package xlingran.reconciler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import xlingran.extension.MomentExtension;
import xlingran.service.ContentEventBridge;
import xlingran.service.MomentFetcher;
import xlingran.service.MomentPushTracker;

@Slf4j
@Component
@RequiredArgsConstructor
public class MomentReconciler implements Reconciler<Reconciler.Request> {

    private final MomentFetcher momentFetcher;
    private final ContentEventBridge contentEventBridge;
    private final MomentPushTracker momentPushTracker;

    @Override
    public Result reconcile(Request request) {
        var name = request.name();
        log.debug("MomentReconciler reconcile name={}", name);
        momentFetcher.fetch(name).ifPresentOrElse(moment -> {
            if (ExtensionUtil.isDeleted(moment)) {
                momentPushTracker.clear(name);
                log.info("MomentReconciler 瞬间删除 name={}", name);
                contentEventBridge.pushMoment(moment, "deleted");
                return;
            }

            momentPushTracker.resolvePushAction(moment).ifPresent(action -> {
                log.info("MomentReconciler 瞬间推送 name={} action={}", name, action);
                contentEventBridge.pushMoment(moment, action);
                momentPushTracker.onPushed(name, moment.getMetadata().getVersion());
            });
        }, () -> log.warn("MomentReconciler 未找到瞬间，跳过推送 name={}", name));
        return new Result(false, null);
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return builder.extension(new MomentExtension()).build();
    }
}

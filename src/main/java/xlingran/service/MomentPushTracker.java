package xlingran.service;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ListOptions;
import xlingran.extension.MomentExtension;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomentPushTracker {

    private final ExtensionClient client;

    private final Set<String> preExistingNames = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, Long> lastPushedVersion = new ConcurrentHashMap<>();
    private volatile boolean snapshotReady = false;

    public void loadExistingMoments() {
        preExistingNames.clear();
        lastPushedVersion.clear();
        var names = client.listAllNames(MomentExtension.class, ListOptions.builder().build(), Sort.unsorted());
        preExistingNames.addAll(names);
        snapshotReady = true;
        log.info("MomentPushTracker 快照已加载，历史瞬间 {} 条", names.size());
    }

    public Optional<String> resolvePushAction(MomentExtension moment) {
        if (!snapshotReady) {
            log.debug("MomentPushTracker 快照未就绪，跳过推送 name={}", moment.getMetadata().getName());
            return Optional.empty();
        }

        var name = moment.getMetadata().getName();
        var version = moment.getMetadata().getVersion();

        var last = lastPushedVersion.get(name);
        if (last != null && last >= version) {
            log.debug("MomentPushTracker 同版本已处理，跳过 name={} version={}", name, version);
            return Optional.empty();
        }

        if (preExistingNames.contains(name) && last == null) {
            lastPushedVersion.put(name, version);
            log.debug("MomentPushTracker 启动同步，仅记录版本 name={} version={}", name, version);
            return Optional.empty();
        }

        var action = preExistingNames.contains(name) ? "updated" : "created";
        return Optional.of(action);
    }

    public void onPushed(String name, long version) {
        lastPushedVersion.put(name, version);
        preExistingNames.add(name);
    }

    public void clear(String name) {
        lastPushedVersion.remove(name);
        preExistingNames.remove(name);
    }
}

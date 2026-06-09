package xlingran.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import run.halo.app.extension.Extension;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.GroupVersionKind;
import run.halo.app.infra.utils.JsonUtils;
import xlingran.extension.MomentExtension;

@Service
@RequiredArgsConstructor
public class MomentFetcher {

    private static final GroupVersionKind MOMENT_GVK =
        GroupVersionKind.fromExtension(MomentExtension.class);

    private final ExtensionClient client;

    public Optional<MomentExtension> fetch(String name) {
        return client.fetch(MOMENT_GVK, name).map(this::toMoment);
    }

    private MomentExtension toMoment(Extension extension) {
        return JsonUtils.DEFAULT_JSON_MAPPER.convertValue(extension, MomentExtension.class);
    }
}

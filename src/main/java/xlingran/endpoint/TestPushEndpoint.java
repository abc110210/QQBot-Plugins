package xlingran.endpoint;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import xlingran.model.TestPushRequest;
import xlingran.service.TestPushService;

@Component
@RequiredArgsConstructor
public class TestPushEndpoint implements CustomEndpoint {

    private final TestPushService testPushService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route(POST("/test-push"), this::testPush);
    }

    @Override
    public GroupVersion groupVersion() {
        return new GroupVersion("console.api.xlingran-shan.halo.run", "v1alpha1");
    }

    private Mono<ServerResponse> testPush(ServerRequest request) {
        return request.bodyToMono(TestPushRequest.class)
            .defaultIfEmpty(new TestPushRequest())
            .flatMap(body -> testPushService.sendTest(body.getTemplateType()))
            .then(ok().bodyValue(Map.of("ok", true)))
            .onErrorResume(IllegalStateException.class,
                e -> ServerResponse.badRequest().bodyValue(Map.of("ok", false, "message", e.getMessage())));
    }
}

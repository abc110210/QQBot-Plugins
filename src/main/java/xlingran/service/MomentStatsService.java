package xlingran.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import run.halo.app.core.extension.Counter;
import run.halo.app.extension.ExtensionClient;

@Service
@RequiredArgsConstructor
public class MomentStatsService {

    private static final String COUNTER_PREFIX = "moments.moment.halo.run/";

    private final ExtensionClient client;

    public Stats statsFor(String momentName) {
        return client.fetch(Counter.class, COUNTER_PREFIX + momentName)
            .map(counter -> new Stats(
                longValue(counter.getUpvote()),
                longValue(counter.getTotalComment()),
                longValue(counter.getApprovedComment())
            ))
            .orElse(new Stats(0L, 0L, 0L));
    }

    private static long longValue(Integer value) {
        return value != null ? value.longValue() : 0L;
    }

    public record Stats(long upvote, long totalComment, long approvedComment) {
    }
}

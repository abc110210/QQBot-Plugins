package xlingran.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import run.halo.app.core.extension.content.Comment;
import run.halo.app.core.extension.content.Reply;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ListOptions;

/**
 * 评论 / 回复推送追踪：启动时对历史资源拍快照，避免插件重启后被全量重推。
 * 机制与 {@link MomentPushTracker} 一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentReplyPushTracker {

    private final ExtensionClient client;

    /** 启动时已存在的评论 / 回复名称（快照），这些不会触发推送。 */
    private final Set<String> preExistingComments = ConcurrentHashMap.newKeySet();
    private final Set<String> preExistingReplies = ConcurrentHashMap.newKeySet();

    /** 本次运行内已推送过的评论 / 回复，防止同一资源被重复调谐时多次推送。 */
    private final Set<String> pushedCommentsInRun = ConcurrentHashMap.newKeySet();
    private final Set<String> pushedRepliesInRun = ConcurrentHashMap.newKeySet();

    private volatile boolean snapshotReady = false;

    /** 插件启动时调用：把所有已存在的评论 / 回复名称记入快照。 */
    public void loadExisting() {
        preExistingComments.clear();
        preExistingReplies.clear();
        pushedCommentsInRun.clear();
        pushedRepliesInRun.clear();
        var comments = client.listAllNames(Comment.class, ListOptions.builder().build(), Sort.unsorted());
        var replies = client.listAllNames(Reply.class, ListOptions.builder().build(), Sort.unsorted());
        preExistingComments.addAll(comments);
        preExistingReplies.addAll(replies);
        snapshotReady = true;
        log.info("CommentReplyPushTracker 快照已加载，历史评论 {} 条，历史回复 {} 条",
            comments.size(), replies.size());
    }

    /** 是否应该推送该评论：快照未就绪或属于历史资源则跳过，并对本次运行做去重。 */
    public boolean shouldPushComment(String name) {
        if (!snapshotReady) {
            log.debug("CommentReplyPushTracker 快照未就绪，跳过推送 comment={}", name);
            return false;
        }
        if (preExistingComments.contains(name)) {
            return false;
        }
        return pushedCommentsInRun.add(name);
    }

    /** 评论删除时清理追踪记录。 */
    public void forgetComment(String name) {
        preExistingComments.remove(name);
        pushedCommentsInRun.remove(name);
    }

    /** 是否应该推送该回复：快照未就绪或属于历史资源则跳过，并对本次运行做去重。 */
    public boolean shouldPushReply(String name) {
        if (!snapshotReady) {
            log.debug("CommentReplyPushTracker 快照未就绪，跳过推送 reply={}", name);
            return false;
        }
        if (preExistingReplies.contains(name)) {
            return false;
        }
        return pushedRepliesInRun.add(name);
    }

    /** 回复删除时清理追踪记录。 */
    public void forgetReply(String name) {
        preExistingReplies.remove(name);
        pushedRepliesInRun.remove(name);
    }
}

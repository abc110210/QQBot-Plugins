package xlingran.service;



import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;

import run.halo.app.core.extension.User;

import run.halo.app.core.extension.content.Comment;

import run.halo.app.extension.ExtensionClient;



@Service

@RequiredArgsConstructor

public class UserDisplayService {



    private final ExtensionClient client;

    private final AttachmentUrlResolver attachmentUrlResolver;



    public String displayName(String username) {

        if (!StringUtils.hasText(username)) {

            return "";

        }

        return client.fetch(User.class, username)

            .map(u -> {

                if (u.getSpec() != null && StringUtils.hasText(u.getSpec().getDisplayName())) {

                    return u.getSpec().getDisplayName();

                }

                return username;

            })

            .orElse(username);

    }



    public String displayName(Comment.CommentOwner owner) {

        if (owner == null) {

            return "";

        }

        if (StringUtils.hasText(owner.getDisplayName())) {

            return owner.getDisplayName();

        }

        if ("User".equals(owner.getKind())) {

            return displayName(owner.getName());

        }

        return owner.getName() != null ? owner.getName() : "";

    }



    public String avatar(String username) {

        if (!StringUtils.hasText(username)) {

            return "";

        }

        return client.fetch(User.class, username)

            .map(this::resolveAvatar)

            .orElse("");

    }



    public String avatar(Comment.CommentOwner owner) {

        if (owner == null) {

            return "";

        }

        if ("User".equals(owner.getKind())) {

            return avatar(owner.getName());

        }

        return "";

    }



    private String resolveAvatar(User user) {

        if (user.getMetadata() != null && user.getMetadata().getAnnotations() != null) {

            var attachmentName = user.getMetadata().getAnnotations()

                .get(User.AVATAR_ATTACHMENT_NAME_ANNO);

            if (StringUtils.hasText(attachmentName)) {

                var fromAttachment = attachmentUrlResolver.resolveByAttachmentName(attachmentName);

                if (StringUtils.hasText(fromAttachment)) {

                    return fromAttachment;

                }

            }

        }

        if (user.getSpec() != null && StringUtils.hasText(user.getSpec().getAvatar())) {

            return attachmentUrlResolver.resolve(user.getSpec().getAvatar());

        }

        return "";

    }

}



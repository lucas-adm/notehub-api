package br.com.notehub.application.dto.notification;

import br.com.notehub.domain.comment.Comment;
import br.com.notehub.domain.flame.Flame;
import br.com.notehub.domain.reply.Reply;
import br.com.notehub.domain.user.User;
import lombok.SneakyThrows;

import java.util.LinkedHashMap;
import java.util.Map;

public record MessageNotification(
        Map<String, Object> info
) {

    private enum Type {
        FOLLOWER,
        FLAME,
        COMMENT,
        REPLY
    }

    @SneakyThrows
    private static Map<String, Object> createInfo(Type type, String target, String message) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("type", type.toString());
        info.put("target", target);
        info.put("message", message);
        return info;
    }

    private static Map<String, Object> createFollowerNotification(User follower) {
        String target = follower.getUsername();
        return createInfo(
                Type.FOLLOWER,
                target,
                String.format("@%s está seguindo você.", target)
        );
    }

    private static Map<String, Object> createFlameNotification(Flame flame) {
        User from = flame.getUser();
        String target = flame.getNote().getId().toString();
        String name = flame.getNote().getName();
        return createInfo(
                Type.FLAME,
                target,
                String.format("@%s inflamou sua nota: %s", from.getUsername(), name)
        );
    }

    private static Map<String, Object> createCommentNotification(Comment comment) {
        User from = comment.getUser();
        String target = comment.getNote().getId().toString();
        String name = comment.getNote().getName();
        return createInfo(
                Type.COMMENT,
                target,
                String.format("@%s comentou em sua nota: %s", from.getUsername(), name)
        );
    }

    private static Map<String, Object> createReplyNotification(Reply reply) {
        User from = reply.getUser();
        String target = reply.getComment().getNote().getId().toString();
        String text = reply.getText();
        if (reply.getToUser() == null) {
            return createInfo(
                    Type.REPLY,
                    target,
                    String.format("@%s respondeu seu comentário: %s", from.getUsername(), text)
            );
        }
        return createInfo(
                Type.REPLY,
                target,
                String.format("@%s respondeu você: %s", from.getUsername(), text)
        );
    }

    public static MessageNotification of(User follower) {
        return new MessageNotification(createFollowerNotification(follower));
    }

    public static MessageNotification of(Flame flame) {
        return new MessageNotification(createFlameNotification(flame));
    }

    public static MessageNotification of(Comment comment) {
        return new MessageNotification(createCommentNotification(comment));
    }

    public static MessageNotification of(Reply reply) {
        return new MessageNotification(createReplyNotification(reply));
    }

}
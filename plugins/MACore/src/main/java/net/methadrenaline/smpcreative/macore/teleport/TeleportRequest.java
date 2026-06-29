package net.methadrenaline.smpcreative.macore.teleport;

import java.util.UUID;

public record TeleportRequest(UUID requesterId, UUID targetId, long expiresAtMillis, int expireTaskId) {
}

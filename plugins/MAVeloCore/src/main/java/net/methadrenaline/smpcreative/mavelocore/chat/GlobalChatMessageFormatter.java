package net.methadrenaline.smpcreative.mavelocore.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.methadrenaline.smpcreative.mavelocore.network.ServerStyle;

public final class GlobalChatMessageFormatter {
    public Component format(String serverId, String username, String message) {
        ServerStyle style = ServerStyle.fromServerId(serverId);
        return Component.text(style.icon(), style.iconColor())
                .append(Component.space())
                .append(Component.text(style.label(), style.labelColor()))
                .append(Component.space())
                .append(Component.text("╹", NamedTextColor.WHITE))
                .append(Component.space())
                .append(Component.text(username, NamedTextColor.WHITE))
                .append(Component.text(" : ", NamedTextColor.GOLD))
                .append(Component.text(message, NamedTextColor.WHITE));
    }
}

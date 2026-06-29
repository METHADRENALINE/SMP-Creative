package net.methadrenaline.smpcreative.macore.chat;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class LocalChatListener implements Listener {
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
    private final GlobalChatBridge globalChatBridge;

    public LocalChatListener(GlobalChatBridge globalChatBridge) {
        this.globalChatBridge = globalChatBridge;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncChat(AsyncChatEvent event) {
        String plainMessage = plain.serialize(event.message());
        if (plainMessage.startsWith("!")) {
            String globalMessage = plainMessage.substring(1).trim();
            event.setCancelled(true);
            if (!globalMessage.isEmpty()) {
                Bukkit.getScheduler().runTask(globalChatBridge.plugin(), () ->
                        globalChatBridge.send(event.getPlayer(), globalMessage));
            }
            return;
        }

        event.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, message) ->
                sourceDisplayName
                        .append(Component.text(" : ", NamedTextColor.GOLD))
                        .append(message.colorIfAbsent(NamedTextColor.WHITE))));
    }
}

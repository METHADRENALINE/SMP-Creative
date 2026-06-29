package net.methadrenaline.smpcreative.macore.chat;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class GlobalChatBridge {
    public static final String CHANNEL = "ma:core";

    private final JavaPlugin plugin;
    private final String serverId;

    public GlobalChatBridge(JavaPlugin plugin, String serverId) {
        this.plugin = plugin;
        this.serverId = serverId;
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public void send(Player player, String message) {
        if (!player.isOnline()) {
            return;
        }

        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteStream);
            out.writeUTF("global-chat");
            out.writeUTF(serverId);
            out.writeUTF(player.getUniqueId().toString());
            out.writeUTF(player.getName());
            out.writeUTF(message);
            player.sendPluginMessage(plugin, CHANNEL, byteStream.toByteArray());
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not send global chat message: " + exception.getMessage());
        }
    }
}

package net.methadrenaline.smpcreative.mavelocore.chat;

import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import net.kyori.adventure.text.Component;
import net.methadrenaline.smpcreative.mavelocore.MAVeloCorePlugin;
import net.methadrenaline.smpcreative.mavelocore.network.ServerId;
import org.slf4j.Logger;

public final class GlobalChatListener {
    private final ProxyServer server;
    private final Logger logger;
    private final GlobalChatMessageFormatter formatter = new GlobalChatMessageFormatter();

    public GlobalChatListener(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    public void handle(PluginMessageEvent event) {
        if (!MAVeloCorePlugin.MACORE_CHANNEL.equals(event.getIdentifier())) {
            return;
        }

        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection sourceConnection)) {
            return;
        }

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String type = input.readUTF();
            if (!"global-chat".equals(type)) {
                return;
            }

            String serverId = ServerId.normalize(input.readUTF());
            input.readUTF();
            String username = input.readUTF();
            String message = input.readUTF();
            if (serverId.isBlank()) {
                serverId = ServerId.normalize(sourceConnection.getServerInfo().getName());
            }

            broadcast(serverId, username, message);
        } catch (IOException | IllegalArgumentException exception) {
            logger.warn("Could not handle MACore plugin message: {}", exception.getMessage());
        }
    }

    private void broadcast(String serverId, String username, String message) {
        Component formatted = formatter.format(serverId, username, message);
        for (Player recipient : server.getAllPlayers()) {
            recipient.sendMessage(formatted);
        }
    }
}

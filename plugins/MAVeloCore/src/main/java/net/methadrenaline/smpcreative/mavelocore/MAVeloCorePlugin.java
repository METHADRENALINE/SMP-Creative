package net.methadrenaline.smpcreative.mavelocore;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.methadrenaline.smpcreative.mavelocore.chat.GlobalChatListener;
import net.methadrenaline.smpcreative.mavelocore.lang.SharedLanguageStore;
import net.methadrenaline.smpcreative.mavelocore.network.NetworkJoinMessages;
import net.methadrenaline.smpcreative.mavelocore.whisper.WhisperCommand;
import org.slf4j.Logger;

@Plugin(
        id = "mavelocore",
        name = "MAVeloCore",
        version = "1.3.1",
        description = "Proxy core for SMP&Creative Velocity network.",
        url = "",
        authors = {"METHADRENALINE"}
)
public final class MAVeloCorePlugin {
    public static final MinecraftChannelIdentifier MACORE_CHANNEL = MinecraftChannelIdentifier.from("ma:core");

    private final ProxyServer server;
    private final Logger logger;
    private final SharedLanguageStore languageStore;
    private final GlobalChatListener globalChatListener;

    @Inject
    public MAVeloCorePlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.languageStore = new SharedLanguageStore(dataDirectory, logger);
        this.globalChatListener = new GlobalChatListener(server, logger);
    }

    @Subscribe(order = PostOrder.LAST)
    public void onProxyInitialize(ProxyInitializeEvent event) {
        languageStore.loadIfChanged();
        server.getChannelRegistrar().register(MACORE_CHANNEL);
        registerWhisperCommands();
        logger.info("MAVeloCore loaded. Shared language file: {}", languageStore.playersFile());
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPluginMessage(PluginMessageEvent event) {
        globalChatListener.handle(event);
    }

    @Subscribe(order = PostOrder.LAST)
    public void onPostLogin(PostLoginEvent event) {
        UUID joinedId = event.getPlayer().getUniqueId();
        String username = event.getPlayer().getUsername();
        server.getScheduler()
                .buildTask(this, () -> server.getPlayer(joinedId)
                        .ifPresent(player -> broadcastLocalized(username, joinedId, true)))
                .delay(750L, TimeUnit.MILLISECONDS)
                .schedule();
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onDisconnect(DisconnectEvent event) {
        if (event.getLoginStatus() != DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN) {
            return;
        }

        Player player = event.getPlayer();
        broadcastLocalized(player.getUsername(), player.getUniqueId(), false);
    }

    private void registerWhisperCommands() {
        CommandManager commandManager = server.getCommandManager();
        commandManager.register(
                commandManager.metaBuilder("msg")
                        .aliases("tell", "w", "whisper")
                        .plugin(this)
                        .build(),
                new WhisperCommand(server)
        );
    }

    private void broadcastLocalized(String username, UUID actorId, boolean join) {
        languageStore.loadIfChanged();

        for (Player recipient : server.getAllPlayers()) {
            if (!join && recipient.getUniqueId().equals(actorId)) {
                continue;
            }

            recipient.sendMessage(NetworkJoinMessages.message(languageStore.languageFor(recipient), username, join));
        }
    }
}

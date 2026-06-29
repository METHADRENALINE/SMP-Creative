package net.methadrenaline.smpcreative.macore;

import net.methadrenaline.smpcreative.macore.chat.GlobalChatBridge;
import net.methadrenaline.smpcreative.macore.chat.LocalChatListener;
import net.methadrenaline.smpcreative.macore.lang.CoreMessages;
import net.methadrenaline.smpcreative.macore.lang.LanguageProvider;
import net.methadrenaline.smpcreative.macore.teleport.TeleportCommand;
import net.methadrenaline.smpcreative.macore.teleport.TeleportRequestService;
import net.methadrenaline.smpcreative.macore.util.ServerId;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MACorePlugin extends JavaPlugin {
    private TeleportRequestService teleportRequests;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String serverId = ServerId.detect(this);
        LanguageProvider languageProvider = new LanguageProvider(this);
        CoreMessages messages = new CoreMessages(languageProvider);
        GlobalChatBridge globalChatBridge = new GlobalChatBridge(this, serverId);
        teleportRequests = new TeleportRequestService(this, messages, serverId);

        Bukkit.getPluginManager().registerEvents(new LocalChatListener(globalChatBridge), this);
        Bukkit.getPluginManager().registerEvents(teleportRequests, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, GlobalChatBridge.CHANNEL);

        TeleportCommand teleportCommand = new TeleportCommand(teleportRequests);
        registerCommand("tpa", teleportCommand);
        registerCommand("tpyes", teleportCommand);
        registerCommand("tpno", teleportCommand);

        getLogger().info("MACore loaded for server id: " + serverId);
    }

    @Override
    public void onDisable() {
        if (teleportRequests != null) {
            teleportRequests.cancelAll();
        }
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this);
    }

    private void registerCommand(String name, TeleportCommand teleportCommand) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(teleportCommand);
            command.setTabCompleter(teleportCommand);
        }
    }
}

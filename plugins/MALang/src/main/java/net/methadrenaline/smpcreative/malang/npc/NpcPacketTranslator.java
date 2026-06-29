package net.methadrenaline.smpcreative.malang.npc;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedTeamParameters;
import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.methadrenaline.smpcreative.malang.MALangPlugin;
import net.methadrenaline.smpcreative.malang.gsit.GSitMessageTranslator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class NpcPacketTranslator {
    private final MALangPlugin plugin;
    private final NpcNameTranslator npcNameTranslator;
    private final GSitMessageTranslator gsitMessageTranslator;
    private boolean installed;

    public NpcPacketTranslator(
            MALangPlugin plugin,
            NpcNameTranslator npcNameTranslator,
            GSitMessageTranslator gsitMessageTranslator
    ) {
        this.plugin = plugin;
        this.npcNameTranslator = npcNameTranslator;
        this.gsitMessageTranslator = gsitMessageTranslator;
    }

    public void install() {
        boolean translateNpcNames = plugin.getConfig().getBoolean("npc-nameplates.enabled", true);
        boolean translateGsitMessages = plugin.getConfig().getBoolean("gsit-sync.enabled", true)
                && gsitMessageTranslator.hasTranslations();
        if (!translateNpcNames && !translateGsitMessages) {
            return;
        }

        Plugin protocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib");
        if (protocolLib == null || !protocolLib.isEnabled()) {
            plugin.getLogger().info("ProtocolLib is not enabled; per-player packet translations were not installed.");
            return;
        }

        try {
            ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                    plugin,
                    ListenerPriority.NORMAL,
                    PacketType.Play.Server.ENTITY_METADATA,
                    PacketType.Play.Server.PLAYER_INFO,
                    PacketType.Play.Server.SCOREBOARD_TEAM,
                    PacketType.Play.Server.SYSTEM_CHAT,
                    PacketType.Play.Server.SET_ACTION_BAR_TEXT
            ) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
                        if (translateNpcNames) {
                            translateNpcNameMetadata(event);
                        }
                    } else if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO) {
                        if (translateNpcNames) {
                            translateNpcPlayerInfo(event);
                        }
                    } else if (event.getPacketType() == PacketType.Play.Server.SCOREBOARD_TEAM) {
                        if (translateNpcNames) {
                            translateNpcScoreboardTeam(event);
                        }
                    } else if (event.getPacketType() == PacketType.Play.Server.SYSTEM_CHAT
                            || event.getPacketType() == PacketType.Play.Server.SET_ACTION_BAR_TEXT) {
                        if (translateGsitMessages) {
                            gsitMessageTranslator.translate(event, NpcPacketTranslator.this.plugin::getFreshLanguageCode, MALangPlugin.ENGLISH);
                        }
                    }
                }
            });
            installed = true;
            plugin.getLogger().info("Installed per-player packet translator (npc-nameplates="
                    + translateNpcNames + ", gsit-sync=" + translateGsitMessages + ").");
        } catch (LinkageError | RuntimeException exception) {
            plugin.getLogger().warning("Could not install per-player packet translator: " + exception.getMessage());
        }
    }

    public void remove() {
        if (!installed) {
            return;
        }

        try {
            ProtocolLibrary.getProtocolManager().removePacketListeners(plugin);
        } catch (LinkageError | RuntimeException ignored) {
        }
        installed = false;
    }

    public void scheduleFancyNpcNameRefresh(Player player, int delayTicks) {
        if (!installed || !plugin.getConfig().getBoolean("npc-nameplates.enabled", true)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                refreshFancyNpcNames(player);
            }
        }, Math.max(1, delayTicks));
    }

    private void translateNpcNameMetadata(PacketEvent event) {
        Player player = event.getPlayer();
        String language = player == null ? MALangPlugin.ENGLISH : plugin.getLanguageCode(player);
        if (MALangPlugin.ENGLISH.equals(language)) {
            return;
        }

        List<WrappedDataValue> values = event.getPacket().getDataValueCollectionModifier().readSafely(0);
        if (values == null || values.isEmpty()) {
            return;
        }

        List<WrappedDataValue> translatedValues = new ArrayList<>(values.size());
        boolean changed = false;

        for (WrappedDataValue value : values) {
            Object original = value.getValue();
            Object translated = translateNpcNameValue(original, language);
            if (translated != original) {
                changed = true;
                translatedValues.add(new WrappedDataValue(value.getIndex(), value.getSerializer(), translated));
            } else {
                translatedValues.add(value);
            }
        }

        if (changed) {
            event.getPacket().getDataValueCollectionModifier().write(0, translatedValues);
        }
    }

    private void translateNpcPlayerInfo(PacketEvent event) {
        Player player = event.getPlayer();
        String language = player == null ? MALangPlugin.ENGLISH : plugin.getLanguageCode(player);
        if (MALangPlugin.ENGLISH.equals(language)) {
            return;
        }

        List<PlayerInfoData> entries = event.getPacket().getPlayerInfoDataLists().readSafely(0);
        if (entries == null || entries.isEmpty()) {
            return;
        }

        List<PlayerInfoData> translatedEntries = new ArrayList<>(entries.size());
        boolean changed = false;
        for (PlayerInfoData entry : entries) {
            if (entry == null) {
                translatedEntries.add(null);
                continue;
            }

            WrappedChatComponent displayName = entry.getDisplayName();
            WrappedChatComponent translatedDisplayName = translateWrappedNpcName(displayName, language);
            WrappedGameProfile profile = entry.getProfile();
            WrappedGameProfile translatedProfile = translateGameProfile(profile, language);

            if (translatedDisplayName != displayName || translatedProfile != profile) {
                changed = true;
                translatedEntries.add(new PlayerInfoData(
                        entry.getProfileId(),
                        entry.getLatency(),
                        entry.isListed(),
                        entry.getGameMode(),
                        translatedProfile,
                        translatedDisplayName,
                        entry.isShowHat(),
                        entry.getListOrder(),
                        entry.getRemoteChatSessionData()
                ));
            } else {
                translatedEntries.add(entry);
            }
        }

        if (changed) {
            event.getPacket().getPlayerInfoDataLists().write(0, translatedEntries);
        }
    }

    private void translateNpcScoreboardTeam(PacketEvent event) {
        Player player = event.getPlayer();
        String language = player == null ? MALangPlugin.ENGLISH : plugin.getLanguageCode(player);
        if (MALangPlugin.ENGLISH.equals(language)) {
            return;
        }

        Optional<WrappedTeamParameters> optionalParameters = event.getPacket().getOptionalTeamParameters().readSafely(0);
        if (optionalParameters == null || optionalParameters.isEmpty()) {
            return;
        }

        WrappedTeamParameters parameters = optionalParameters.get();
        WrappedChatComponent displayName = parameters.getDisplayName();
        WrappedChatComponent prefix = parameters.getPrefix();
        WrappedChatComponent suffix = parameters.getSuffix();
        WrappedChatComponent translatedDisplayName = translateWrappedNpcName(displayName, language);
        WrappedChatComponent translatedPrefix = translateWrappedNpcName(prefix, language);
        WrappedChatComponent translatedSuffix = translateWrappedNpcName(suffix, language);
        if (translatedDisplayName == displayName && translatedPrefix == prefix && translatedSuffix == suffix) {
            return;
        }

        WrappedTeamParameters.Builder builder = WrappedTeamParameters.newBuilder(parameters);
        if (translatedDisplayName != displayName) {
            builder.displayName(translatedDisplayName);
        }
        if (translatedPrefix != prefix) {
            builder.prefix(translatedPrefix);
        }
        if (translatedSuffix != suffix) {
            builder.suffix(translatedSuffix);
        }
        event.getPacket().getOptionalTeamParameters().write(0, Optional.of(builder.build()));
    }

    private Object translateNpcNameValue(Object value, String language) {
        if (value instanceof Optional<?> optional) {
            if (optional.isEmpty()) {
                return value;
            }

            Object original = optional.get();
            Object translated = translateNpcNameComponent(original, language);
            return translated == original ? value : Optional.of(translated);
        }

        return translateNpcNameComponent(value, language);
    }

    private Object translateNpcNameComponent(Object value, String language) {
        if (value instanceof WrappedChatComponent component) {
            WrappedChatComponent translated = translateWrappedNpcName(component, language);
            return translated == component ? value : translated.getHandle();
        }

        if (value == null) {
            return null;
        }

        try {
            WrappedChatComponent component = WrappedChatComponent.fromHandle(value);
            WrappedChatComponent translated = translateWrappedNpcName(component, language);
            return translated == component ? value : translated.getHandle();
        } catch (RuntimeException ignored) {
            return value;
        }
    }

    private WrappedGameProfile translateGameProfile(WrappedGameProfile profile, String language) {
        if (profile == null) {
            return null;
        }

        String name = profile.getName();
        if (name == null || name.isEmpty()) {
            return profile;
        }

        String translatedName = npcNameTranslator.replace(name, language);
        return translatedName.equals(name) ? profile : profile.withName(translatedName);
    }

    private WrappedChatComponent translateWrappedNpcName(WrappedChatComponent component, String language) {
        if (component == null) {
            return null;
        }

        String json = component.getJson();
        String translated = npcNameTranslator.replace(json, language);
        return translated.equals(json) ? component : WrappedChatComponent.fromJson(translated);
    }

    private void refreshFancyNpcNames(Player player) {
        Plugin fancyNpcs = Bukkit.getPluginManager().getPlugin("FancyNpcs");
        if (fancyNpcs == null || !fancyNpcs.isEnabled()) {
            return;
        }

        try {
            for (Npc npc : FancyNpcsPlugin.get().getNpcManager().getAllNpcs()) {
                String displayName = npc.getData().getDisplayName();
                if (displayName != null && npcNameTranslator.isTranslated(displayName) && npc.isShownFor(player)) {
                    npc.update(player, true);
                }
            }
        } catch (LinkageError | RuntimeException exception) {
            plugin.getLogger().warning("Could not refresh FancyNpcs nameplates for " + player.getName() + ": " + exception.getMessage());
        }
    }
}

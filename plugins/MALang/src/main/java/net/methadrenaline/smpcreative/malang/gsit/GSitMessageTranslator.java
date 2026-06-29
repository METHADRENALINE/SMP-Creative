package net.methadrenaline.smpcreative.malang.gsit;

import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class GSitMessageTranslator {
    private final Map<String, Map<String, Component>> translations = new HashMap<>();
    private final GsonComponentSerializer gsonSerializer = GsonComponentSerializer.gson();
    private final PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
    private final LegacyComponentSerializer legacyAmpersandSerializer = LegacyComponentSerializer.legacyAmpersand();

    public boolean hasTranslations() {
        return !translations.isEmpty();
    }

    public void load(File langFolder, Logger logger, String english, String ukrainian, String russian) {
        translations.clear();
        if (!langFolder.isDirectory()) {
            return;
        }

        Map<String, YamlConfiguration> languages = new HashMap<>();
        languages.put(english, YamlConfiguration.loadConfiguration(new File(langFolder, "en_us.yml")));
        languages.put(ukrainian, YamlConfiguration.loadConfiguration(new File(langFolder, "uk_ua.yml")));
        languages.put(russian, YamlConfiguration.loadConfiguration(new File(langFolder, "ru_ru.yml")));

        Set<String> keys = new HashSet<>();
        for (YamlConfiguration languageConfig : languages.values()) {
            collectMessageKeys(languageConfig, "Plugin", keys);
            collectMessageKeys(languageConfig, "Messages", keys);
        }

        for (String key : keys) {
            Map<String, String> rawByLanguage = new HashMap<>();
            boolean hasPlaceholders = false;
            for (Map.Entry<String, YamlConfiguration> entry : languages.entrySet()) {
                String raw = rawMessage(entry.getValue(), key);
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                if (raw.contains("%")) {
                    hasPlaceholders = true;
                }
                rawByLanguage.put(entry.getKey(), raw);
            }

            if (hasPlaceholders || rawByLanguage.isEmpty()) {
                continue;
            }

            Map<String, Component> targets = new HashMap<>();
            for (Map.Entry<String, String> target : rawByLanguage.entrySet()) {
                targets.put(target.getKey(), deserializeTarget(target.getValue(), target.getKey(), english));
            }

            for (Map.Entry<String, String> source : rawByLanguage.entrySet()) {
                for (String rawVariant : sourceVariants(source.getValue(), source.getKey(), english)) {
                    String plain = normalizePlain(plainSerializer.serialize(legacyAmpersandSerializer.deserialize(rawVariant)));
                    if (!plain.isEmpty()) {
                        translations.put(plain, targets);
                    }
                }
            }
        }

        if (!translations.isEmpty()) {
            logger.info("Loaded " + translations.size() + " GSit message translation source(s).");
        }
    }

    public void translate(PacketEvent event, Function<Player, String> languageProvider, String english) {
        if (translations.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        WrappedChatComponent wrapped = event.getPacket().getChatComponents().readSafely(0);
        if (wrapped == null) {
            return;
        }

        Component component;
        try {
            component = gsonSerializer.deserialize(wrapped.getJson());
        } catch (RuntimeException exception) {
            return;
        }

        String plain = normalizePlain(plainSerializer.serialize(component));
        Map<String, Component> targets = translations.get(plain);
        if (targets == null) {
            return;
        }

        String language = languageProvider.apply(player);
        Component translated = targets.get(language);
        if (translated == null) {
            translated = targets.get(english);
        }
        if (translated == null) {
            return;
        }

        event.getPacket().getChatComponents().write(0, WrappedChatComponent.fromJson(gsonSerializer.serialize(translated)));
    }

    private void collectMessageKeys(YamlConfiguration config, String sectionName, Set<String> keys) {
        ConfigurationSection section = config.getConfigurationSection(sectionName);
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(true)) {
            String path = sectionName + "." + key;
            if (config.isString(path)) {
                keys.add(path);
            }
        }
    }

    private String rawMessage(YamlConfiguration config, String key) {
        String raw = config.getString(key);
        if (raw == null) {
            return null;
        }

        String prefix = config.getString("Plugin.plugin-prefix", "&7[&6GSit&7]");
        return raw.replace("[P]", prefix);
    }

    private Component deserializeTarget(String raw, String language, String english) {
        return legacyAmpersandSerializer.deserialize(replaceClientTranslation(raw, sneakLabel(language, english)));
    }

    private List<String> sourceVariants(String raw, String language, String english) {
        String normalized = raw == null ? "" : raw;
        if (!normalized.contains("<lang:key.sneak>")) {
            return List.of(normalized);
        }

        Set<String> variants = new HashSet<>();
        variants.add(replaceClientTranslation(normalized, sneakLabel(language, english)));
        variants.add(replaceClientTranslation(normalized, "Sneak"));
        variants.add(replaceClientTranslation(normalized, "Shift"));
        variants.add(replaceClientTranslation(normalized, "key.sneak"));
        return new ArrayList<>(variants);
    }

    private String replaceClientTranslation(String raw, String replacement) {
        return (raw == null ? "" : raw).replace("<lang:key.sneak>", replacement);
    }

    private String sneakLabel(String language, String english) {
        return english.equals(language) ? "Sneak" : "Shift";
    }

    private static String normalizePlain(String text) {
        return text == null ? "" : text.replace("\r\n", "\n").trim();
    }
}

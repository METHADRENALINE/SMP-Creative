package net.methadrenaline.smpcreative.madialogs.dialog;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import net.methadrenaline.smpcreative.madialogs.lang.LanguageCode;

public final class DialogRegistry {
    private Map<String, NpcDialog> dialogs = Map.of();
    private DialogSettings settings = DialogSettings.defaults();

    public DialogSettings settings() {
        return settings;
    }

    public NpcDialog get(String id) {
        return dialogs.get(normalizeId(id));
    }

    public Set<String> ids() {
        return dialogs.keySet();
    }

    public int size() {
        return dialogs.size();
    }

    public void reload(File file, Logger logger) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        settings = DialogSettings.from(config.getConfigurationSection("settings"));

        Map<String, NpcDialog> loaded = new HashMap<>();
        ConfigurationSection dialogsSection = config.getConfigurationSection("dialogs");
        if (dialogsSection == null) {
            logger.warning("No dialogs section found in dialogs.yml");
            dialogs = Map.of();
            return;
        }

        for (String id : dialogsSection.getKeys(false)) {
            ConfigurationSection section = dialogsSection.getConfigurationSection(id);
            if (section == null) {
                continue;
            }

            NpcDialog dialog = loadDialog(id, section, logger);
            if (dialog != null) {
                loaded.put(dialog.id(), dialog);
            }
        }

        dialogs = Map.copyOf(loaded);
        logger.info("Loaded " + dialogs.size() + " NPC dialogs.");
    }

    private NpcDialog loadDialog(String id, ConfigurationSection section, Logger logger) {
        List<String> pages = section.getStringList("pages");
        if (pages.isEmpty()) {
            logger.warning("Skipping dialog '" + id + "' because it has no pages.");
            return null;
        }

        String normalizedId = normalizeId(id);
        DialogVariant defaultVariant = new DialogVariant(
                section.getString("title", id),
                List.copyOf(pages)
        );

        return new NpcDialog(
                normalizedId,
                defaultVariant,
                loadTranslations(id, section, defaultVariant, logger),
                loadPageSounds(id, section, logger)
        );
    }

    private Map<String, DialogVariant> loadTranslations(String id, ConfigurationSection section, DialogVariant defaultVariant, Logger logger) {
        Map<String, DialogVariant> translations = new HashMap<>();
        ConfigurationSection translationsSection = section.getConfigurationSection("translations");
        if (translationsSection == null) {
            return Map.of();
        }

        for (String language : translationsSection.getKeys(false)) {
            ConfigurationSection translationSection = translationsSection.getConfigurationSection(language);
            if (translationSection == null) {
                continue;
            }

            List<String> translatedPages = translationSection.getStringList("pages");
            if (translatedPages.isEmpty()) {
                logger.warning("Ignoring translation '" + language + "' in dialog '" + id + "' because it has no pages.");
                continue;
            }

            translations.put(LanguageCode.normalize(language), new DialogVariant(
                    translationSection.getString("title", defaultVariant.title()),
                    List.copyOf(translatedPages)
            ));
        }

        return Map.copyOf(translations);
    }

    private Map<Integer, List<String>> loadPageSounds(String id, ConfigurationSection section, Logger logger) {
        Map<Integer, List<String>> pageSounds = new HashMap<>();
        ConfigurationSection soundsSection = section.getConfigurationSection("page-sounds");
        if (soundsSection == null) {
            return Map.of();
        }

        for (String page : soundsSection.getKeys(false)) {
            try {
                pageSounds.put(Integer.parseInt(page), soundsSection.getStringList(page));
            } catch (NumberFormatException ignored) {
                logger.warning("Ignoring non-numeric page-sounds key '" + page + "' in dialog '" + id + "'.");
            }
        }

        return Map.copyOf(pageSounds);
    }

    private static String normalizeId(String id) {
        return id.toLowerCase(Locale.ROOT);
    }
}

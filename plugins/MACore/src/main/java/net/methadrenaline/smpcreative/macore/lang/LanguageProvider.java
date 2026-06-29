package net.methadrenaline.smpcreative.macore.lang;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class LanguageProvider {
    public static final String ENGLISH = "en";
    public static final String UKRAINIAN = "ua";
    public static final String RUSSIAN = "ru";

    private final JavaPlugin plugin;

    public LanguageProvider(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String languageFor(Player player) {
        Plugin langPlugin = Bukkit.getPluginManager().getPlugin("MALang");
        if (langPlugin != null && langPlugin.isEnabled()) {
            try {
                Method method = langPlugin.getClass().getMethod("getLanguageCode", Player.class);
                Object result = method.invoke(langPlugin, player);
                if (result instanceof String language) {
                    return normalize(language);
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
                plugin.getLogger().warning("Could not read language from MALang: " + exception.getMessage());
            }
        }

        return normalize(player.getLocale());
    }

    public static String normalize(String language) {
        if (language == null) {
            return ENGLISH;
        }

        String normalized = language.toLowerCase(Locale.ROOT).replace('-', '_');
        if (normalized.equals("ua") || normalized.equals("uk") || normalized.equals("uk_ua") || normalized.startsWith("uk_")) {
            return UKRAINIAN;
        }
        if (normalized.equals("ru") || normalized.equals("ru_ru") || normalized.startsWith("ru_")) {
            return RUSSIAN;
        }
        return ENGLISH;
    }
}

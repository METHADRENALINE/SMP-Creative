package net.methadrenaline.smpcreative.madialogs.lang;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class LanguageResolver {
    private LanguageResolver() {
    }

    public static String resolve(Player player, Logger logger) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("MALang");
        if (plugin != null && plugin.isEnabled()) {
            try {
                Method method = plugin.getClass().getMethod("getLanguageCode", Player.class);
                Object result = method.invoke(plugin, player);
                if (result instanceof String language) {
                    return LanguageCode.normalize(language);
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
                logger.warning("Could not read language from MALang: " + exception.getMessage());
            }
        }

        return LanguageCode.detect(player.getLocale());
    }
}

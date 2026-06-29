package net.methadrenaline.smpcreative.maaura.util;

import java.io.File;
import java.util.Locale;
import org.bukkit.plugin.java.JavaPlugin;

public final class ServerId {
    private ServerId() {
    }

    public static String detect(JavaPlugin plugin) {
        String configured = plugin.getConfig().getString("server.id", "auto");
        if (configured != null && !"auto".equalsIgnoreCase(configured)) {
            return normalize(configured);
        }

        try {
            return normalize(new File(".").getCanonicalFile().getName());
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    public static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
    }
}

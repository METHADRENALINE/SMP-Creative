package net.methadrenaline.smpcreative.mavelocore.lang;

import com.velocitypowered.api.proxy.Player;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;

public final class SharedLanguageStore {
    private static final String PLAYERS_FILE_PROPERTY = "mavelocore.playersFile";

    private final Path playersFile;
    private final Logger logger;
    private final Map<UUID, String> overrides = new HashMap<>();
    private long playersFileLastModified = Long.MIN_VALUE;

    public SharedLanguageStore(Path dataDirectory, Logger logger) {
        this.playersFile = Path.of(System.getProperty(
                PLAYERS_FILE_PROPERTY,
                dataDirectory.resolve("players.yml").toString()
        ));
        this.logger = logger;
    }

    public Path playersFile() {
        return playersFile;
    }

    public String languageFor(Player player) {
        String override = overrides.get(player.getUniqueId());
        if (override != null) {
            return override;
        }

        Locale locale = player.hasSentPlayerSettings()
                ? player.getPlayerSettings().getLocale()
                : player.getEffectiveLocale();
        return LanguageCode.detect(locale);
    }

    public void loadIfChanged() {
        try {
            long lastModified = Files.exists(playersFile) ? Files.getLastModifiedTime(playersFile).toMillis() : -1L;
            if (lastModified == playersFileLastModified) {
                return;
            }

            playersFileLastModified = lastModified;
            overrides.clear();
            if (!Files.exists(playersFile)) {
                return;
            }

            readPlayersFile();
        } catch (IOException exception) {
            logger.warn("Could not read shared language file {}: {}", playersFile, exception.getMessage());
        }
    }

    private void readPlayersFile() throws IOException {
        String currentUuid = null;
        for (String line : Files.readAllLines(playersFile)) {
            if (line.startsWith("  ") && !line.startsWith("    ") && line.trim().endsWith(":")) {
                currentUuid = line.trim();
                currentUuid = currentUuid.substring(0, currentUuid.length() - 1);
                continue;
            }

            if (currentUuid == null || !line.startsWith("    language:")) {
                continue;
            }

            String language = LanguageCode.normalize(line.substring(line.indexOf(':') + 1).trim());
            if (LanguageCode.isSupported(language)) {
                storeOverride(currentUuid, language);
            }
        }
    }

    private void storeOverride(String uuidText, String language) {
        try {
            overrides.put(UUID.fromString(uuidText), language);
        } catch (IllegalArgumentException ignored) {
            logger.warn("Ignoring invalid UUID in language file: {}", uuidText);
        }
    }
}

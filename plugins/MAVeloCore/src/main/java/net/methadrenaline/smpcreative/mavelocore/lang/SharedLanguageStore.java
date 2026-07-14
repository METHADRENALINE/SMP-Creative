package net.methadrenaline.smpcreative.mavelocore.lang;

import com.velocitypowered.api.proxy.Player;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.slf4j.Logger;

public final class SharedLanguageStore implements AutoCloseable {
    private static final String PLAYERS_FILE_PROPERTY = "mavelocore.playersFile";
    private static final String STORAGE_TYPE_PROPERTY = "mavelocore.storage.type";
    private static final String DATABASE_PROPERTIES_FILE_PROPERTY = "mavelocore.storage.postgres.propertiesFile";

    private final Path playersFile;
    private final Logger logger;
    private final String storageType;
    private volatile Map<UUID, String> languages = Map.of();
    private HikariDataSource dataSource;
    private String tableName;
    private long playersFileLastModified = Long.MIN_VALUE;
    private long nextPostgresReloadAt;

    public SharedLanguageStore(Path dataDirectory, Logger logger) {
        playersFile = Path.of(System.getProperty(
                PLAYERS_FILE_PROPERTY,
                dataDirectory.resolve("players.yml").toString()
        ));
        storageType = System.getProperty(STORAGE_TYPE_PROPERTY, "yaml").trim().toLowerCase(Locale.ROOT);
        this.logger = logger;
    }

    public String storageDescription() {
        return postgresEnabled() ? "PostgreSQL" : playersFile.toString();
    }

    public String languageFor(Player player) {
        String stored = languages.get(player.getUniqueId());
        if (stored != null) {
            return stored;
        }

        Locale locale = player.hasSentPlayerSettings()
                ? player.getPlayerSettings().getLocale()
                : player.getEffectiveLocale();
        return LanguageCode.detect(locale);
    }

    public synchronized void loadIfChanged() {
        if (postgresEnabled()) {
            loadPostgresIfChanged();
            return;
        }
        loadYamlIfChanged();
    }

    @Override
    public synchronized void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    private void loadPostgresIfChanged() {
        long now = System.currentTimeMillis();
        if (dataSource != null && now < nextPostgresReloadAt) {
            return;
        }
        nextPostgresReloadAt = now + 1000L;
        try {
            if (dataSource == null) {
                initializePool();
                try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                    statement.executeUpdate("create table if not exists " + tableName + " (player_uuid uuid primary key, player_name text not null, language_code text not null check (language_code in ('en', 'ua', 'ru')), source text not null check (source in ('client', 'manual')), updated_at timestamptz not null default now())");
                }
            }
            Map<UUID, String> loaded = new HashMap<>();
            try (Connection connection = dataSource.getConnection();
                    Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery("select player_uuid, language_code from " + tableName)) {
                while (result.next()) {
                    String language = LanguageCode.normalize(result.getString("language_code"));
                    if (LanguageCode.isSupported(language)) {
                        loaded.put(result.getObject("player_uuid", UUID.class), language);
                    }
                }
            }
            languages = Map.copyOf(loaded);
        } catch (Exception exception) {
            logger.warn("Could not refresh PostgreSQL language storage: {}", exception.getMessage());
        }
    }

    private void initializePool() throws IOException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("PostgreSQL JDBC driver is missing", exception);
        }
        Properties properties = new Properties();
        String propertiesFile = System.getProperty(DATABASE_PROPERTIES_FILE_PROPERTY, "");
        if (!propertiesFile.isBlank()) {
            try (Reader reader = Files.newBufferedReader(Path.of(propertiesFile), StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        }

        String jdbcUrl = property(properties, "jdbc-url", System.getProperty("mavelocore.storage.postgres.jdbcUrl", ""));
        String username = property(properties, "username", System.getProperty("mavelocore.storage.postgres.username", ""));
        String password = property(properties, "password", System.getProperty("mavelocore.storage.postgres.password", ""));
        String prefix = tablePrefix(System.getProperty("mavelocore.storage.postgres.tablePrefix", "ma_"));
        if (jdbcUrl.isBlank()) {
            throw new IllegalStateException("mavelocore.storage.postgres.jdbcUrl is empty");
        }
        tableName = prefix + "player_languages";

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        if (!username.isBlank()) {
            config.setUsername(username);
            config.setPassword(password);
        }
        int maximumSize = positiveInt("mavelocore.storage.postgres.pool.maximumSize", 2);
        config.setMaximumPoolSize(maximumSize);
        config.setMinimumIdle(Math.min(maximumSize, nonNegativeInt("mavelocore.storage.postgres.pool.minimumIdle", 1)));
        config.setConnectionTimeout(positiveLong("mavelocore.storage.postgres.pool.connectionTimeoutMs", 3000L, 250L));
        config.setValidationTimeout(positiveLong("mavelocore.storage.postgres.pool.validationTimeoutMs", 1000L, 250L));
        config.setIdleTimeout(positiveLong("mavelocore.storage.postgres.pool.idleTimeoutMs", 600000L, 10000L));
        config.setMaxLifetime(positiveLong("mavelocore.storage.postgres.pool.maxLifetimeMs", 1800000L, 30000L));
        config.setKeepaliveTime(positiveLong("mavelocore.storage.postgres.pool.keepaliveTimeMs", 300000L, 30000L));
        config.setPoolName("MAVeloCore-PostgreSQL");
        dataSource = new HikariDataSource(config);
    }

    private void loadYamlIfChanged() {
        try {
            long lastModified = Files.exists(playersFile) ? Files.getLastModifiedTime(playersFile).toMillis() : -1L;
            if (lastModified == playersFileLastModified) {
                return;
            }
            playersFileLastModified = lastModified;
            languages = Files.exists(playersFile) ? readPlayersFile() : Map.of();
        } catch (IOException exception) {
            logger.warn("Could not read shared language file {}: {}", playersFile, exception.getMessage());
        }
    }

    private Map<UUID, String> readPlayersFile() throws IOException {
        Map<UUID, String> loaded = new HashMap<>();
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
                storeLanguage(loaded, currentUuid, language);
            }
        }
        return Map.copyOf(loaded);
    }

    private void storeLanguage(Map<UUID, String> target, String uuidText, String language) {
        try {
            target.put(UUID.fromString(uuidText), language);
        } catch (IllegalArgumentException ignored) {
            logger.warn("Ignoring invalid UUID in language file: {}", uuidText);
        }
    }

    private boolean postgresEnabled() {
        return "postgres".equals(storageType) || "postgresql".equals(storageType);
    }

    private static String property(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String tablePrefix(String configured) {
        String value = configured == null || configured.isBlank() ? "ma_" : configured.toLowerCase(Locale.ROOT);
        return value.matches("[a-z0-9_]+") ? value : "ma_";
    }

    private static int positiveInt(String property, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(System.getProperty(property, Integer.toString(fallback))));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static int nonNegativeInt(String property, int fallback) {
        try {
            return Math.max(0, Integer.parseInt(System.getProperty(property, Integer.toString(fallback))));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static long positiveLong(String property, long fallback, long minimum) {
        try {
            return Math.max(minimum, Long.parseLong(System.getProperty(property, Long.toString(fallback))));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}

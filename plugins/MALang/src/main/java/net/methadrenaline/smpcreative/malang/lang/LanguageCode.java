package net.methadrenaline.smpcreative.malang.lang;

import java.util.Locale;
import java.util.Set;

public final class LanguageCode {
    public static final String ENGLISH = "en";
    public static final String UKRAINIAN = "ua";
    public static final String RUSSIAN = "ru";
    public static final Set<String> SUPPORTED = Set.of(ENGLISH, UKRAINIAN, RUSSIAN);

    private LanguageCode() {
    }

    public static String normalize(String language) {
        if (language == null) {
            return ENGLISH;
        }

        String normalized = language.toLowerCase(Locale.ROOT).replace('-', '_');
        if (normalized.equals("uk") || normalized.equals("uk_ua") || normalized.equals("ua")) {
            return UKRAINIAN;
        }
        if (normalized.equals("ru") || normalized.equals("ru_ru") || normalized.startsWith("ru_")) {
            return RUSSIAN;
        }
        if (normalized.startsWith("en")) {
            return ENGLISH;
        }
        if (normalized.equals("auto")) {
            return "auto";
        }
        return ENGLISH;
    }

    public static boolean isSupported(String language) {
        return language != null && SUPPORTED.contains(language);
    }

    public static String detect(String locale) {
        if (locale == null) {
            return ENGLISH;
        }

        String normalized = locale.toLowerCase(Locale.ROOT).replace('-', '_');
        if (normalized.equals("uk") || normalized.equals("uk_ua") || normalized.startsWith("uk_")) {
            return UKRAINIAN;
        }
        if (normalized.equals("ru") || normalized.equals("ru_ru") || normalized.startsWith("ru_")) {
            return RUSSIAN;
        }
        return ENGLISH;
    }

    public static String displayName(String language) {
        return switch (language) {
            case UKRAINIAN -> "Українська";
            case RUSSIAN -> "Русский";
            default -> "English";
        };
    }
}

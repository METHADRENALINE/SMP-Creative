package net.methadrenaline.smpcreative.mavelocore.lang;

import java.util.Locale;

public final class LanguageCode {
    public static final String ENGLISH = "en";
    public static final String UKRAINIAN = "ua";
    public static final String RUSSIAN = "ru";

    private LanguageCode() {
    }

    public static String normalize(String language) {
        if (language == null) {
            return ENGLISH;
        }

        String normalized = stripQuotes(language).toLowerCase(Locale.ROOT).replace('-', '_');
        if (normalized.equals("uk") || normalized.equals("uk_ua") || normalized.equals("ua")) {
            return UKRAINIAN;
        }
        if (normalized.equals("ru") || normalized.equals("ru_ru") || normalized.startsWith("ru_")) {
            return RUSSIAN;
        }
        if (normalized.startsWith("en")) {
            return ENGLISH;
        }
        return ENGLISH;
    }

    public static boolean isSupported(String language) {
        return ENGLISH.equals(language) || UKRAINIAN.equals(language) || RUSSIAN.equals(language);
    }

    public static String detect(Locale locale) {
        if (locale == null || locale.getLanguage() == null) {
            return ENGLISH;
        }

        if (locale.getLanguage().equalsIgnoreCase("uk")) {
            return UKRAINIAN;
        }
        if (locale.getLanguage().equalsIgnoreCase("ru")) {
            return RUSSIAN;
        }
        return ENGLISH;
    }

    private static String stripQuotes(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}

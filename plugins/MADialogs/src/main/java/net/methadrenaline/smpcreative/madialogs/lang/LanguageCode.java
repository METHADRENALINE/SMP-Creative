package net.methadrenaline.smpcreative.madialogs.lang;

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

        String normalized = language.toLowerCase(Locale.ROOT).replace('-', '_');
        if (normalized.equals("ua") || normalized.equals("uk") || normalized.startsWith("uk_")) {
            return UKRAINIAN;
        }
        if (normalized.equals("ru") || normalized.startsWith("ru_")) {
            return RUSSIAN;
        }
        return ENGLISH;
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
}

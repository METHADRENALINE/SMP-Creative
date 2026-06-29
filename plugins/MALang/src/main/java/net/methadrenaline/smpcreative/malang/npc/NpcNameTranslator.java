package net.methadrenaline.smpcreative.malang.npc;

import java.util.Map;
import net.methadrenaline.smpcreative.malang.lang.LanguageCode;

public final class NpcNameTranslator {
    private static final Map<String, Map<String, String>> NAMES = Map.of(
            LanguageCode.UKRAINIAN, Map.of(
                    "Shelley", "Шеллі",
                    "Klyment-02", "Климент-02",
                    "Boris", "Борис",
                    "Thomas", "Томас"
            ),
            LanguageCode.RUSSIAN, Map.of(
                    "Shelley", "Шелли",
                    "Klyment-02", "Климент-02",
                    "Boris", "Борис",
                    "Thomas", "Томас"
            )
    );

    public String replace(String text, String language) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        Map<String, String> names = NAMES.get(language);
        if (names == null || names.isEmpty()) {
            return text;
        }

        String translated = text;
        for (Map.Entry<String, String> entry : names.entrySet()) {
            translated = translated.replace(entry.getKey(), entry.getValue());
        }
        return translated;
    }

    public boolean isTranslated(String name) {
        return NAMES.values().stream().anyMatch(names -> names.containsKey(name) || names.containsValue(name));
    }
}

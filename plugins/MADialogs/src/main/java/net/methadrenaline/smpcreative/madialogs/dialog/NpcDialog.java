package net.methadrenaline.smpcreative.madialogs.dialog;

import java.util.List;
import java.util.Map;
import net.methadrenaline.smpcreative.madialogs.lang.LanguageCode;

public record NpcDialog(
        String id,
        DialogVariant defaultVariant,
        Map<String, DialogVariant> translations,
        Map<Integer, List<String>> pageSounds
) {
    public DialogVariant variant(String language) {
        return translations.getOrDefault(LanguageCode.normalize(language), defaultVariant);
    }
}

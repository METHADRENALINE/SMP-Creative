package net.methadrenaline.smpcreative.mavelocore.network;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.methadrenaline.smpcreative.mavelocore.lang.LanguageCode;

public final class NetworkJoinMessages {
    private NetworkJoinMessages() {
    }

    public static Component message(String language, String username, boolean join) {
        String text = switch (language) {
            case LanguageCode.UKRAINIAN -> join ? " зайшов на сервер" : " вийшов із сервера";
            case LanguageCode.RUSSIAN -> join ? " зашёл на сервер" : " вышел с сервера";
            default -> join ? " has connected" : " has disconnected";
        };

        return Component.text(username, NamedTextColor.GOLD)
                .append(Component.text(text, NamedTextColor.WHITE));
    }
}

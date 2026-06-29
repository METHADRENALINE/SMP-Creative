package net.methadrenaline.smpcreative.macore.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public final class CoreMessages {
    private final LanguageProvider languageProvider;
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

    public CoreMessages(LanguageProvider languageProvider) {
        this.languageProvider = languageProvider;
    }

    public Component component(Player player, String key) {
        return legacy.deserialize(text(languageProvider.languageFor(player), key));
    }

    public String languageFor(Player player) {
        return languageProvider.languageFor(player);
    }

    public String text(String language, String key) {
        return switch (LanguageProvider.normalize(language)) {
            case LanguageProvider.UKRAINIAN -> ukrainian(key);
            case LanguageProvider.RUSSIAN -> russian(key);
            default -> english(key);
        };
    }

    private String ukrainian(String key) {
        return switch (key) {
            case "only-modes" -> "&6Ця команда працює тільки в режимах &cSMP &6та &bCreative&6!";
            case "tpa-usage" -> "&cВикористання: /tpa <гравець>";
            case "player-not-found" -> "&cГравця %player% не знайдено.";
            case "self-request" -> "&cНе можна надіслати запит самому собі.";
            case "request-sent" -> "&6Запит на телепортацію до &c%player% &6надіслано.";
            case "request-received" -> "&6%player% &6хоче телепортуватися до тебе.";
            case "timeout" -> "&6Цей запит зникне через &c%seconds% секунд&6.";
            case "no-request" -> "&cУ тебе немає активних запитів на телепортацію.";
            case "requester-offline" -> "&cГравець, який надіслав запит, уже не онлайн.";
            case "denied-target" -> "&6Запит від &c%player% &6відхилено.";
            case "denied-requester" -> "&c%player% &6відхилив твій запит на телепортацію.";
            case "accepted-target" -> "&6Запит від &c%player% &6прийнято.";
            case "accepted-requester" -> "&6%player% &6прийняв твій запит. Телепортуємо...";
            case "expired-requester" -> "&cТвій запит на телепортацію до %player% минув.";
            case "expired-target" -> "&cЗапит на телепортацію від %player% минув.";
            case "teleport-failed" -> "&cТелепортація не вдалася.";
            default -> "";
        };
    }

    private String russian(String key) {
        return switch (key) {
            case "only-modes" -> "&6Данная команда работает только на &cSMP &6и &bCreative &6режимах!";
            case "tpa-usage" -> "&cИспользование: /tpa <игрок>";
            case "player-not-found" -> "&cИгрок %player% не найден.";
            case "self-request" -> "&cНельзя отправить запрос самому себе.";
            case "request-sent" -> "&6Запрос на телепортацию к &c%player% &6отправлен.";
            case "request-received" -> "&6%player% &6хочет телепортироваться к тебе.";
            case "timeout" -> "&6Этот запрос истечёт через &c%seconds% секунд&6.";
            case "no-request" -> "&cУ тебя нет активных запросов на телепортацию.";
            case "requester-offline" -> "&cИгрок, который отправил запрос, уже не онлайн.";
            case "denied-target" -> "&6Запрос от &c%player% &6отклонён.";
            case "denied-requester" -> "&c%player% &6отклонил твой запрос на телепортацию.";
            case "accepted-target" -> "&6Запрос от &c%player% &6принят.";
            case "accepted-requester" -> "&6%player% &6принял твой запрос. Телепортируем...";
            case "expired-requester" -> "&cТвой запрос на телепортацию к %player% истёк.";
            case "expired-target" -> "&cЗапрос на телепортацию от %player% истёк.";
            case "teleport-failed" -> "&cТелепортация не удалась.";
            default -> "";
        };
    }

    private String english(String key) {
        return switch (key) {
            case "only-modes" -> "&6This command works only in &cSMP &6and &bCreative &6modes!";
            case "tpa-usage" -> "&cUsage: /tpa <player>";
            case "player-not-found" -> "&cPlayer %player% was not found.";
            case "self-request" -> "&cYou cannot send a teleport request to yourself.";
            case "request-sent" -> "&6Teleport request sent to &c%player%&6.";
            case "request-received" -> "&6%player% &6has requested to teleport to you.";
            case "timeout" -> "&6This request will timeout after &c%seconds% seconds&6.";
            case "no-request" -> "&cYou do not have any pending teleport requests.";
            case "requester-offline" -> "&cThe player who sent this request is no longer online.";
            case "denied-target" -> "&6Teleport request from &c%player% &6denied.";
            case "denied-requester" -> "&c%player% &6denied your teleport request.";
            case "accepted-target" -> "&6Teleport request from &c%player% &6accepted.";
            case "accepted-requester" -> "&6%player% &6accepted your teleport request. Teleporting...";
            case "expired-requester" -> "&cYour teleport request to %player% has expired.";
            case "expired-target" -> "&cTeleport request from %player% has expired.";
            case "teleport-failed" -> "&cTeleportation failed.";
            default -> "";
        };
    }
}

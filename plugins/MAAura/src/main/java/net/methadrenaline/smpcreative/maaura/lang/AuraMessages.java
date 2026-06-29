package net.methadrenaline.smpcreative.maaura.lang;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import net.methadrenaline.smpcreative.maaura.aura.AuraCatalog;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class AuraMessages {
    public static final String ENGLISH = "en";
    public static final String UKRAINIAN = "ua";
    public static final String RUSSIAN = "ru";

    private final JavaPlugin plugin;

    public AuraMessages(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String languageFor(Player player) {
        Plugin langPlugin = Bukkit.getPluginManager().getPlugin("MALang");
        if (langPlugin != null && langPlugin.isEnabled()) {
            try {
                Method method = langPlugin.getClass().getMethod("getLanguageCode", Player.class);
                Object result = method.invoke(langPlugin, player);
                if (result instanceof String language) {
                    return normalizeLanguage(language);
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
                plugin.getLogger().warning("Could not read language from MALang: " + exception.getMessage());
            }
        }

        return normalizeLanguage(player.getLocale());
    }

    public String auraName(Player player, String auraId) {
        String language = languageFor(player);
        return switch (auraId) {
            case AuraCatalog.FIND_ZEN -> switch (language) {
                case UKRAINIAN -> "Спіймати дзен";
                case RUSSIAN -> "Поймать дзен";
                default -> "Find your zen";
            };
            case AuraCatalog.LOYALTY -> switch (language) {
                case UKRAINIAN -> "Аура вірності";
                case RUSSIAN -> "Аура верности";
                default -> "Loyalty aura";
            };
            default -> auraId;
        };
    }

    public List<String> auraInfoLore(Player player) {
        return switch (languageFor(player)) {
            case UKRAINIAN -> List.of(
                    "&fЦе, передусім, рідкісний косметичний ефект",
                    "&fдля вашого персонажа. Його можна отримати",
                    "&fза участь у подіях та інших штуках"
            );
            case RUSSIAN -> List.of(
                    "&fЭто, прежде всего, редкий косметический эффект",
                    "&fдля вашего персонажа. Получается за участие",
                    "&fв событиях и прочих штуках"
            );
            default -> List.of(
                    "&fThis is, first of all, a rare cosmetic effect",
                    "&ffor your character. You get it by taking part",
                    "&fin events and other things"
            );
        };
    }

    public String text(Player player, String key) {
        return text(languageFor(player), key);
    }

    public String text(String language, String key) {
        return switch (normalizeLanguage(language)) {
            case UKRAINIAN -> ukrainian(key);
            case RUSSIAN -> russian(key);
            default -> english(key);
        };
    }

    public String scopeLabel(Player player, String scope) {
        return switch (scope) {
            case "lobby" -> "&6Lobby";
            case "survival" -> "&cSMP";
            case "creative" -> "&bCreative";
            default -> "&f" + text(player, "all");
        };
    }

    public String scopeDescription(Player player, String scope) {
        String language = languageFor(player);
        if ("all".equals(scope)) {
            return switch (language) {
                case UKRAINIAN -> "Вашу ауру видно в усіх режимах";
                case RUSSIAN -> "Вашу ауру видно во всех режимах";
                default -> "Your aura is visible in all modes";
            };
        }

        String mode = scopeLabel(player, scope);
        return switch (language) {
            case UKRAINIAN -> "Вашу ауру видно тільки в режимі " + mode;
            case RUSSIAN -> "Вашу ауру видно только в режиме " + mode;
            default -> "Your aura is visible only in " + mode;
        };
    }

    public static String normalizeLanguage(String language) {
        if (language == null) {
            return ENGLISH;
        }

        String normalized = language.toLowerCase(Locale.ROOT).replace('-', '_');
        if (normalized.equals("ua") || normalized.equals("uk") || normalized.equals("uk_ua") || normalized.startsWith("uk_")) {
            return UKRAINIAN;
        }
        if (normalized.equals("ru") || normalized.equals("ru_ru") || normalized.startsWith("ru_")) {
            return RUSSIAN;
        }
        return ENGLISH;
    }

    private String ukrainian(String key) {
        return switch (key) {
            case "aura-button", "menu-title" -> "Аура";
            case "aura-button-desc" -> "Меню керування аурами";
            case "aura-info" -> "Аура";
            case "catalog", "catalog-title" -> "Каталог";
            case "settings", "settings-title" -> "Налаштування";
            case "visibility", "visibility-title" -> "Видимість";
            case "scope" -> "Видимість у режимах";
            case "visibility-desc" -> "Хто і де бачить аури";
            case "privacy" -> "Конфіденційність";
            case "private" -> "Приватний";
            case "public" -> "Публічний";
            case "private-desc" -> "Тільки ви бачите свою ауру";
            case "public-desc" -> "Усі бачать вашу ауру";
            case "others-aura" -> "Чужа аура";
            case "others-desc" -> "Показує або приховує чужі аури тільки для вас";
            case "own-aura" -> "Своя аура";
            case "own-desc" -> "Показує або приховує вашу ауру тільки для вас";
            case "owners" -> "Власники";
            case "enabled" -> "Увімкнено";
            case "disabled" -> "Вимкнено";
            case "not-owner" -> "Ця аура вам недоступна";
            case "mode" -> "Режим";
            case "all" -> "Усі";
            case "back" -> "Назад";
            case "redeem" -> "Активувати";
            case "redeem-desc" -> "Активувати код аури";
            case "redeem-hint" -> "Введи код";
            case "redeem-input" -> "Введіть код у форматі XXXXX-XXXXX-XXXXX-XXXXX-XXXXX";
            case "redeem-invalid" -> "Такого коду не існує або його вже активовано.";
            case "aura-unlocked" -> "Ауру активовано";
            default -> key;
        };
    }

    private String russian(String key) {
        return switch (key) {
            case "aura-button", "menu-title" -> "Аура";
            case "aura-button-desc" -> "Меню управления аурами";
            case "aura-info" -> "Аура";
            case "catalog", "catalog-title" -> "Каталог";
            case "settings", "settings-title" -> "Настройки";
            case "visibility", "visibility-title" -> "Видимость";
            case "scope" -> "Видимость в режимах";
            case "visibility-desc" -> "Кто и где видит ауры";
            case "privacy" -> "Конфиденциальность";
            case "private" -> "Приватный";
            case "public" -> "Публичный";
            case "private-desc" -> "Только вы видите свою ауру";
            case "public-desc" -> "Все видят вашу ауру";
            case "others-aura" -> "Чужая аура";
            case "others-desc" -> "Показывает или скрывает чужие ауры только для вас";
            case "own-aura" -> "Своя аура";
            case "own-desc" -> "Показывает или скрывает вашу ауру только для вас";
            case "owners" -> "Владельцы";
            case "enabled" -> "Включено";
            case "disabled" -> "Отключено";
            case "not-owner" -> "Эта аура вам недоступна";
            case "mode" -> "Режим";
            case "all" -> "Все";
            case "back" -> "Назад";
            case "redeem" -> "Активировать";
            case "redeem-desc" -> "Активировать код ауры";
            case "redeem-hint" -> "Введи код";
            case "redeem-input" -> "Введите код в формате XXXXX-XXXXX-XXXXX-XXXXX-XXXXX";
            case "redeem-invalid" -> "Такого кода не существует либо он уже был активирован.";
            case "aura-unlocked" -> "Аура активирована";
            default -> key;
        };
    }

    private String english(String key) {
        return switch (key) {
            case "aura-button", "menu-title" -> "Aura";
            case "aura-button-desc" -> "Aura management menu";
            case "aura-info" -> "Aura";
            case "catalog", "catalog-title" -> "Catalog";
            case "settings", "settings-title" -> "Settings";
            case "visibility", "visibility-title" -> "Visibility";
            case "scope" -> "Mode visibility";
            case "visibility-desc" -> "Who can see auras and where";
            case "privacy" -> "Privacy";
            case "private" -> "Private";
            case "public" -> "Public";
            case "private-desc" -> "Only you can see your aura";
            case "public-desc" -> "Everyone can see your aura";
            case "others-aura" -> "Other auras";
            case "others-desc" -> "Shows or hides other players' auras only for you";
            case "own-aura" -> "Own aura";
            case "own-desc" -> "Shows or hides your aura only for you";
            case "owners" -> "Owners";
            case "enabled" -> "Enabled";
            case "disabled" -> "Disabled";
            case "not-owner" -> "This aura is not available to you";
            case "mode" -> "Mode";
            case "all" -> "All";
            case "back" -> "Back";
            case "redeem" -> "Redeem";
            case "redeem-desc" -> "Redeem your aura code";
            case "redeem-hint" -> "Enter code";
            case "redeem-input" -> "Enter a code like XXXXX-XXXXX-XXXXX-XXXXX-XXXXX";
            case "redeem-invalid" -> "This code does not exist or has already been redeemed.";
            case "aura-unlocked" -> "Aura unlocked";
            default -> key;
        };
    }
}

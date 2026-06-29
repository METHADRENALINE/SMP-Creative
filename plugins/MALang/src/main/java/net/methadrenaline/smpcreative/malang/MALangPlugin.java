package net.methadrenaline.smpcreative.malang;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.methadrenaline.smpcreative.malang.gsit.GSitMessageTranslator;
import net.methadrenaline.smpcreative.malang.lang.LanguageCode;
import net.methadrenaline.smpcreative.malang.menu.LanguageSelectorHolder;
import net.methadrenaline.smpcreative.malang.menu.ModeSelectorHolder;
import net.methadrenaline.smpcreative.malang.npc.NpcNameTranslator;
import net.methadrenaline.smpcreative.malang.npc.NpcPacketTranslator;
import net.methadrenaline.smpcreative.malang.signs.LobbySignTranslator;
import net.methadrenaline.smpcreative.malang.storage.PlayerLanguageStorage;
import net.methadrenaline.smpcreative.malang.util.ItemFactory;

public final class MALangPlugin extends JavaPlugin implements Listener, TabExecutor {
    public static final String ENGLISH = LanguageCode.ENGLISH;
    public static final String UKRAINIAN = LanguageCode.UKRAINIAN;
    public static final String RUSSIAN = LanguageCode.RUSSIAN;
    private static final int LANGUAGE_ENGLISH_SLOT = 11;
    private static final int LANGUAGE_UKRAINIAN_SLOT = 13;
    private static final int LANGUAGE_RUSSIAN_SLOT = 15;
    private static final int LANGUAGE_AUTO_SLOT = 31;
    private static final String EARTH_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTI4OWQ1YjE3ODYyNmVhMjNkMGIwYzNkMmRmNWMwODVlODM3NTA1NmJmNjg1YjVlZDViYjQ3N2ZlODQ3MmQ5NCJ9fX0=";
    private static final String AUTO_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTgxOGU4YmMzNTNlODgzNmQzNGFmYWQ2YzQzOGMxMjQ0M2ViZGU5MGRlMjRiYWFkYTlmMmM4NTJjMzFkZjJhYyJ9fX0=";
    private static final String UK_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODc5ZDk5ZDljNDY0NzRlMjcxM2E3ZTg0YTk1ZTRjZTdlOGZmOGVhNGQxNjQ0MTNhNTkyZTQ0MzVkMmM2ZjlkYyJ9fX0=";
    private static final String UA_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjhiOWY1MmUzNmFhNWM3Y2FhYTFlN2YyNmVhOTdlMjhmNjM1ZThlYWM5YWVmNzRjZWM5N2Y0NjVmNWE2YjUxIn19fQ==";

    private final Map<UUID, Integer> pendingWelcomeTasks = new HashMap<>();
    private final LegacyComponentSerializer legacyAmpersandSerializer = LegacyComponentSerializer.legacyAmpersand();
    private final GSitMessageTranslator gsitMessageTranslator = new GSitMessageTranslator();
    private final ItemFactory itemFactory = new ItemFactory(legacyAmpersandSerializer);
    private final NpcNameTranslator npcNameTranslator = new NpcNameTranslator();
    private final NpcPacketTranslator npcPacketTranslator = new NpcPacketTranslator(this, npcNameTranslator, gsitMessageTranslator);
    private final PlayerLanguageStorage languageStorage = new PlayerLanguageStorage(this);
    private final LobbySignTranslator lobbySignTranslator = new LobbySignTranslator(this);
    private NamespacedKey selectorKey;
    private NamespacedKey languageItemKey;


    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureLanguageSelectorDefaults();
        languageStorage.load();
        selectorKey = new NamespacedKey(this, "mode_selector");
        languageItemKey = new NamespacedKey(this, "language_selector");

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        gsitMessageTranslator.load(new File(getConfig().getString("gsit-sync.lang-folder", "plugins/GSit/lang")), getLogger(), ENGLISH, UKRAINIAN, RUSSIAN);
        npcPacketTranslator.install();

        PluginCommand command = getCommand("lang");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            scheduleSelectorItem(player, 2);
            scheduleModeSelectorHeldSlot(player, 4);
            scheduleLanguageItem(player, 2);
            lobbySignTranslator.schedule(player, 20);
            npcPacketTranslator.scheduleFancyNpcNameRefresh(player, 20);
        }
        getLogger().info("Loaded " + languageStorage.manualCount() + " language override(s) and "
                + languageStorage.clientCount() + " client language(s).");
    }

    private void ensureLanguageSelectorDefaults() {
        boolean changed = false;
        changed |= setDefaultConfigValue("gsit-sync.lang-folder", "plugins/GSit/lang");
        changed |= setDefaultConfigValue("language-selector.menu-size", 45);
        changed |= setDefaultConfigValue("language-selector.filler.material", "GRAY_STAINED_GLASS_PANE");
        changed |= setDefaultConfigValue("language-selector.filler.display-name", " ");

        changed |= setDefaultConfigValue("language-selector.languages.en.item-name", "&6Language");
        changed |= setDefaultConfigValue("language-selector.languages.en.item-lore",
                List.of("&7Choose the language the server will use to talk to you"));
        changed |= setDefaultConfigValue("language-selector.languages.en.menu-title", "Language");
        changed |= setDefaultConfigValue("language-selector.languages.en.auto-name", "&6Automatic");
        changed |= setDefaultConfigValue("language-selector.languages.en.auto-lore",
                List.of("&7Your client language will be used"));
        changed |= setDefaultConfigValue("language-selector.languages.en.en-name", "&6English");
        changed |= setDefaultConfigValue("language-selector.languages.en.en-lore", List.of());
        changed |= setDefaultConfigValue("language-selector.languages.en.ua-name", "&6Українська");
        changed |= setDefaultConfigValue("language-selector.languages.en.ua-lore", List.of());
        changed |= setDefaultConfigValue("language-selector.languages.en.ru-name", "&6Русский");
        changed |= setDefaultConfigValue("language-selector.languages.en.ru-lore", List.of());
        changed |= setDefaultConfigValue("language-selector.languages.en.selected", "Selected");

        changed |= setDefaultConfigValue("language-selector.languages.ua.item-name", "&6Мова");
        changed |= setDefaultConfigValue("language-selector.languages.ua.item-lore",
                List.of("&7Виберіть мову, якою сервер спілкуватиметься з вами"));
        changed |= setDefaultConfigValue("language-selector.languages.ua.menu-title", "Мова");
        changed |= setDefaultConfigValue("language-selector.languages.ua.auto-name", "&6Автоматично");
        changed |= setDefaultConfigValue("language-selector.languages.ua.auto-lore",
                List.of("&7Буде вибрано мову вашого клієнта"));
        changed |= setDefaultConfigValue("language-selector.languages.ua.en-name", "&6English");
        changed |= setDefaultConfigValue("language-selector.languages.ua.en-lore", List.of());
        changed |= setDefaultConfigValue("language-selector.languages.ua.ua-name", "&6Українська");
        changed |= setDefaultConfigValue("language-selector.languages.ua.ua-lore", List.of());
        changed |= setDefaultConfigValue("language-selector.languages.ua.ru-name", "&6Русский");
        changed |= setDefaultConfigValue("language-selector.languages.ua.ru-lore", List.of());
        changed |= setDefaultConfigValue("language-selector.languages.ua.selected", "Вибрано");

        changed |= setDefaultConfigValue("language-selector.languages.ru.item-name", "&6Язык");
        changed |= setDefaultConfigValue("language-selector.languages.ru.item-lore",
                List.of("&7Выберите язык, на котором сервер будет общаться с вами"));
        changed |= setDefaultConfigValue("language-selector.languages.ru.menu-title", "Язык");
        changed |= setDefaultConfigValue("language-selector.languages.ru.auto-name", "&6Автоматически");
        changed |= setDefaultConfigValue("language-selector.languages.ru.auto-lore",
                List.of("&7Будет выбран язык вашего клиента"));
        changed |= setDefaultConfigValue("language-selector.languages.ru.en-name", "&6English");
        changed |= setDefaultConfigValue("language-selector.languages.ru.en-lore", List.of());
        changed |= setDefaultConfigValue("language-selector.languages.ru.ua-name", "&6Українська");
        changed |= setDefaultConfigValue("language-selector.languages.ru.ua-lore", List.of());
        changed |= setDefaultConfigValue("language-selector.languages.ru.ru-name", "&6Русский");
        changed |= setDefaultConfigValue("language-selector.languages.ru.ru-lore", List.of());
        changed |= setDefaultConfigValue("language-selector.languages.ru.selected", "Выбран");

        if (changed) {
            saveConfig();
        }
    }

    private boolean setDefaultConfigValue(String path, Object value) {
        if (getConfig().isSet(path)) {
            return false;
        }
        getConfig().set(path, value);
        return true;
    }

    @Override
    public void onDisable() {
        for (int taskId : pendingWelcomeTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        pendingWelcomeTasks.clear();
        npcPacketTranslator.remove();
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this);
    }

    public String getLanguageCode(Player player) {
        return languageStorage.getLanguageCode(player);
    }

    public String getFreshLanguageCode(Player player) {
        return languageStorage.getFreshLanguageCode(player);
    }

    public boolean isUkrainian(Player player) {
        return UKRAINIAN.equals(getLanguageCode(player));
    }

    public boolean isRussian(Player player) {
        return RUSSIAN.equals(getLanguageCode(player));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        languageStorage.load();
        scheduleClientLanguageSync(event.getPlayer(), 5);

        if (!getConfig().getBoolean("welcome-title.enabled", true)) {
            scheduleSelectorItem(event.getPlayer(), 10);
            scheduleModeSelectorHeldSlot(event.getPlayer(), 30);
            scheduleLanguageItem(event.getPlayer(), 10);
            lobbySignTranslator.schedule(event.getPlayer(), getConfig().getInt("lobby-signs.refresh-delay-ticks", 20));
            npcPacketTranslator.scheduleFancyNpcNameRefresh(event.getPlayer(), 20);
            return;
        }

        scheduleWelcome(event.getPlayer(), getConfig().getInt("welcome-title.delay-ticks", 40));
        scheduleSelectorItem(event.getPlayer(), 10);
        scheduleModeSelectorHeldSlot(event.getPlayer(), 30);
        scheduleLanguageItem(event.getPlayer(), 10);
        lobbySignTranslator.schedule(event.getPlayer(), getConfig().getInt("lobby-signs.refresh-delay-ticks", 20));
        npcPacketTranslator.scheduleFancyNpcNameRefresh(event.getPlayer(), 20);
    }

    @EventHandler
    public void onLocaleChange(PlayerLocaleChangeEvent event) {
        if (pendingWelcomeTasks.containsKey(event.getPlayer().getUniqueId())) {
            scheduleWelcome(event.getPlayer(), 2);
        }

        if (!languageStorage.hasManualOverride(event.getPlayer().getUniqueId())) {
            scheduleClientLanguageSync(event.getPlayer(), 1);
            scheduleSelectorItem(event.getPlayer(), 2);
            scheduleLanguageItem(event.getPlayer(), 2);
            lobbySignTranslator.schedule(event.getPlayer(), 2);
            npcPacketTranslator.scheduleFancyNpcNameRefresh(event.getPlayer(), 2);
        }
    }

    @EventHandler
    public void onPlayerChunkLoad(PlayerChunkLoadEvent event) {
        lobbySignTranslator.schedule(event.getPlayer(), event.getChunk(), getConfig().getInt("lobby-signs.chunk-refresh-delay-ticks", 10));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /" + label + ".");
            return true;
        }

        if (args.length == 0) {
            openLanguageSelector(player);
            return true;
        }

        String requested = LanguageCode.normalize(args[0]);
        if ("auto".equals(requested)) {
            setAutoLanguage(player, true);
            return true;
        }

        if (!LanguageCode.isSupported(requested)) {
            player.sendMessage(color(message(getLanguageCode(player), "usage")));
            return true;
        }

        setManualLanguage(player, requested, true);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }

        List<String> options = new ArrayList<>(List.of("ua", "ru", "en", "auto"));
        String prefix = args[0].toLowerCase(Locale.ROOT);
        options.removeIf(option -> !option.startsWith(prefix));
        Collections.sort(options);
        return options;
    }

    private void scheduleWelcome(Player player, int delayTicks) {
        UUID uuid = player.getUniqueId();
        Integer previousTask = pendingWelcomeTasks.remove(uuid);
        if (previousTask != null) {
            Bukkit.getScheduler().cancelTask(previousTask);
        }

        int taskId = Bukkit.getScheduler().runTaskLater(this, () -> {
            pendingWelcomeTasks.remove(uuid);
            if (player.isOnline()) {
                showWelcome(player);
            }
        }, Math.max(1, delayTicks)).getTaskId();

        pendingWelcomeTasks.put(uuid, taskId);
    }

    private void showWelcome(Player player) {
        String language = getLanguageCode(player);
        String basePath = "welcome-title.languages." + language + ".";
        if (!getConfig().isConfigurationSection("welcome-title.languages." + language)) {
            basePath = "welcome-title.languages." + ENGLISH + ".";
        }

        String title = getConfig().getString(basePath + "title", "&3Give 'em heaps,");
        String subtitle = getConfig().getString(basePath + "subtitle", "&6%player%!");
        title = title.replace("%player%", player.getName());
        subtitle = subtitle.replace("%player%", player.getName());

        player.sendTitle(
                color(title),
                color(subtitle),
                getConfig().getInt("welcome-title.fade-in", 45),
                getConfig().getInt("welcome-title.stay", 100),
                getConfig().getInt("welcome-title.fade-out", 20)
        );
    }

    private void scheduleClientLanguageSync(Player player, int delayTicks) {
        if (!getConfig().getBoolean("settings.auto-detect-client-language", true)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(this, () -> syncClientLanguage(player), Math.max(1, delayTicks));
    }

    private void syncClientLanguage(Player player) {
        if (!player.isOnline()) {
            return;
        }

        languageStorage.loadIfChanged();
        if (languageStorage.hasManualOverride(player.getUniqueId())) {
            return;
        }

        String detected = LanguageCode.detect(player.getLocale());
        String previous = languageStorage.setClientLanguage(player.getUniqueId(), detected);
        if (!detected.equals(previous)) {
            refreshPlayerLanguageVisuals(player);
        }
    }

    @EventHandler
    public void onInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (isSelectorItem(event.getItem())) {
            event.setCancelled(true);
            openModeSelector(event.getPlayer());
        } else if (isLanguageItem(event.getItem())) {
            event.setCancelled(true);
            openLanguageSelector(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ModeSelectorHolder && event.getWhoClicked() instanceof Player player) {
            event.setCancelled(true);
            int rawSlot = event.getRawSlot();
            if (rawSlot == serverSlot("survival")) {
                connect(player, getConfig().getString("mode-selector.servers.survival.server", "survival"));
            } else if (rawSlot == serverSlot("creative")) {
                connect(player, getConfig().getString("mode-selector.servers.creative.server", "creative"));
            }
            return;
        }

        if (event.getInventory().getHolder() instanceof LanguageSelectorHolder && event.getWhoClicked() instanceof Player player) {
            event.setCancelled(true);
            handleLanguageSelectionClick(player, event.getRawSlot());
            return;
        }

        if (isProtectedHotbarItem(event.getCurrentItem()) || isProtectedHotbarItem(event.getCursor())
                || isProtectedHotbarItem(event.getHotbarButton() >= 0 ? event.getWhoClicked().getInventory().getItem(event.getHotbarButton()) : null)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ModeSelectorHolder
                || event.getInventory().getHolder() instanceof LanguageSelectorHolder
                || isProtectedHotbarItem(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (isProtectedHotbarItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (isProtectedHotbarItem(event.getMainHandItem()) || isProtectedHotbarItem(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    private void scheduleSelectorItem(Player player, int delayTicks) {
        if (!getConfig().getBoolean("mode-selector.enabled", true)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                giveSelectorItem(player);
            }
        }, Math.max(1, delayTicks));
    }

    private void giveSelectorItem(Player player) {
        int slot = Math.max(0, Math.min(8, getConfig().getInt("mode-selector.slot", 4)));
        player.getInventory().setItem(slot, selectorItem(player));
    }

    private void scheduleModeSelectorHeldSlot(Player player, int delayTicks) {
        if (!getConfig().getBoolean("mode-selector.enabled", true)
                || !getConfig().getBoolean("mode-selector.select-on-join", true)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                player.getInventory().setHeldItemSlot(Math.max(0, Math.min(8, getConfig().getInt("mode-selector.slot", 4))));
            }
        }, Math.max(1, delayTicks));
    }

    private ItemStack selectorItem(Player player) {
        ItemStack item = new ItemStack(itemFactory.material(getConfig(), "mode-selector.material", Material.NETHER_STAR));
        ItemMeta meta = item.getItemMeta();
        itemFactory.applyItemText(meta, languageString(player, "item-name", "Mode Selection"),
                languageStringList(player, "item-lore", List.of("&7Game server selection")));
        meta.getPersistentDataContainer().set(selectorKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void scheduleLanguageItem(Player player, int delayTicks) {
        if (!languageSelectorEnabled()) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                giveLanguageItem(player);
            }
        }, Math.max(1, delayTicks));
    }

    private void giveLanguageItem(Player player) {
        int slot = Math.max(0, Math.min(8, getConfig().getInt("language-selector.slot", 6)));
        player.getInventory().setItem(slot, languageHotbarItem(player));
    }

    private ItemStack languageHotbarItem(Player player) {
        ItemStack item = itemFactory.customHead(EARTH_TEXTURE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            itemFactory.applyItemText(meta, languageMenuString(player, "item-name", "Language"),
                    languageMenuStringList(player, "item-lore", List.of("&7Choose the language the server will use to talk to you")));
            meta.getPersistentDataContainer().set(languageItemKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void openModeSelector(Player player) {
        int size = Math.max(9, Math.min(54, getConfig().getInt("mode-selector.menu-size", 27)));
        size = ((size + 8) / 9) * 9;

        Inventory inventory = Bukkit.createInventory(
                new ModeSelectorHolder(),
                size,
                color(languageString(player, "menu-title", "Mode Selection"))
        );

        ItemStack filler = new ItemStack(itemFactory.material(getConfig(), "mode-selector.filler.material", Material.GRAY_STAINED_GLASS_PANE));
        ItemMeta fillerMeta = filler.getItemMeta();
        itemFactory.applyItemText(fillerMeta, getConfig().getString("mode-selector.filler.display-name", " "), List.of());
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < size; i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(serverSlot("survival"), serverItem(player, "survival", "survival-lore"));
        inventory.setItem(serverSlot("creative"), serverItem(player, "creative", "creative-lore"));
        player.openInventory(inventory);
    }

    private ItemStack serverItem(Player player, String id, String loreKey) {
        String base = "mode-selector.servers." + id + ".";
        ItemStack item = new ItemStack(itemFactory.material(getConfig(), base + "material", Material.STONE));
        ItemMeta meta = item.getItemMeta();
        itemFactory.applyItemText(meta, getConfig().getString(base + "display-name", id), languageStringList(player, loreKey, List.of()));
        item.setItemMeta(meta);
        return item;
    }

    private void openLanguageSelector(Player player) {
        int size = Math.max(45, Math.min(54, getConfig().getInt("language-selector.menu-size", 45)));
        size = Math.max(45, ((size + 8) / 9) * 9);
        Inventory inventory = Bukkit.createInventory(
                new LanguageSelectorHolder(),
                size,
                color(languageMenuString(player, "menu-title", "Language"))
        );

        ItemStack filler = new ItemStack(itemFactory.material(getConfig(), "language-selector.filler.material", Material.GRAY_STAINED_GLASS_PANE));
        ItemMeta fillerMeta = filler.getItemMeta();
        itemFactory.applyItemText(fillerMeta, getConfig().getString("language-selector.filler.display-name", " "), List.of());
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(LANGUAGE_AUTO_SLOT, languageOptionItem(player, "auto", itemFactory.customHead(AUTO_TEXTURE)));
        inventory.setItem(LANGUAGE_ENGLISH_SLOT, languageOptionItem(player, ENGLISH, itemFactory.customHead(UK_TEXTURE)));
        inventory.setItem(LANGUAGE_UKRAINIAN_SLOT, languageOptionItem(player, UKRAINIAN, itemFactory.customHead(UA_TEXTURE)));
        inventory.setItem(LANGUAGE_RUSSIAN_SLOT, languageOptionItem(player, RUSSIAN, new ItemStack(Material.WITHER_SKELETON_SKULL)));
        player.openInventory(inventory);
    }

    private ItemStack languageOptionItem(Player player, String option, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>(languageMenuStringList(player, option + "-lore", List.of()));
            if (languageOptionSelected(player, option)) {
                lore.add("&a" + languageMenuString(player, "selected", "Selected"));
            }
            itemFactory.applyItemText(meta, languageMenuString(player, option + "-name", option), lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void handleLanguageSelectionClick(Player player, int slot) {
        if (slot == LANGUAGE_AUTO_SLOT) {
            setAutoLanguage(player, false);
        } else if (slot == LANGUAGE_ENGLISH_SLOT) {
            setManualLanguage(player, ENGLISH, false);
        } else if (slot == LANGUAGE_UKRAINIAN_SLOT) {
            setManualLanguage(player, UKRAINIAN, false);
        } else if (slot == LANGUAGE_RUSSIAN_SLOT) {
            setManualLanguage(player, RUSSIAN, false);
        } else {
            return;
        }
        player.closeInventory();
    }

    private int serverSlot(String id) {
        return getConfig().getInt("mode-selector.servers." + id + ".slot", "creative".equals(id) ? 11 : 15);
    }

    private void connect(Player player, String server) {
        player.closeInventory();
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(this, "BungeeCord", bytes.toByteArray());
        } catch (IOException exception) {
            getLogger().warning("Could not connect " + player.getName() + " to " + server + ": " + exception.getMessage());
        }
    }

    private boolean isSelectorItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(selectorKey, PersistentDataType.BYTE);
    }

    private boolean isLanguageItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(languageItemKey, PersistentDataType.BYTE);
    }

    private boolean isProtectedHotbarItem(ItemStack item) {
        return isSelectorItem(item) || isLanguageItem(item);
    }

    private boolean languageOptionSelected(Player player, String option) {
        if ("auto".equals(option)) {
            return !languageStorage.hasManualOverride(player.getUniqueId());
        }
        return option.equals(languageStorage.manualOverride(player.getUniqueId()));
    }

    private void setAutoLanguage(Player player, boolean notify) {
        languageStorage.setAutoLanguage(player);
        if (notify) {
            String current = getLanguageCode(player);
            player.sendMessage(color(message(current, "auto-set")
                    .replace("%language%", LanguageCode.displayName(current))));
        }
        refreshPlayerLanguageVisuals(player);
    }

    private void setManualLanguage(Player player, String language, boolean notify) {
        languageStorage.setManualLanguage(player, language);
        if (notify) {
            player.sendMessage(color(message(language, "language-set")
                    .replace("%language%", LanguageCode.displayName(language))));
        }
        refreshPlayerLanguageVisuals(player);
    }

    private void refreshPlayerLanguageVisuals(Player player) {
        scheduleSelectorItem(player, 1);
        scheduleLanguageItem(player, 1);
        lobbySignTranslator.schedule(player, 1);
        npcPacketTranslator.scheduleFancyNpcNameRefresh(player, 1);
        refreshAuraHotbarItem(player);
    }

    private void refreshAuraHotbarItem(Player player) {
        Plugin auraPlugin = Bukkit.getPluginManager().getPlugin("MAAura");
        if (auraPlugin == null || !auraPlugin.isEnabled()) {
            return;
        }

        try {
            Method method = auraPlugin.getClass().getMethod("refreshLobbyItem", Player.class);
            method.invoke(auraPlugin, player);
        } catch (NoSuchMethodException ignored) {
        } catch (IllegalAccessException | InvocationTargetException exception) {
            getLogger().warning("Could not refresh MAAura hotbar item for "
                    + player.getName() + ": " + exception.getMessage());
        }
    }

    private String languageString(Player player, String key, String fallback) {
        String language = getLanguageCode(player);
        String path = "mode-selector.languages." + language + "." + key;
        if (!getConfig().isSet(path)) {
            path = "mode-selector.languages." + ENGLISH + "." + key;
        }
        return getConfig().getString(path, fallback);
    }

    private List<String> languageStringList(Player player, String key, List<String> fallback) {
        String language = getLanguageCode(player);
        String path = "mode-selector.languages." + language + "." + key;
        if (!getConfig().isList(path)) {
            path = "mode-selector.languages." + ENGLISH + "." + key;
        }
        List<String> configured = getConfig().getStringList(path);
        return configured.isEmpty() ? fallback : configured;
    }

    private String languageMenuString(Player player, String key, String fallback) {
        String language = getLanguageCode(player);
        String path = "language-selector.languages." + language + "." + key;
        if (!getConfig().isSet(path)) {
            path = "language-selector.languages." + ENGLISH + "." + key;
        }
        return getConfig().getString(path, fallback);
    }

    private List<String> languageMenuStringList(Player player, String key, List<String> fallback) {
        String language = getLanguageCode(player);
        String path = "language-selector.languages." + language + "." + key;
        if (!getConfig().isList(path)) {
            path = "language-selector.languages." + ENGLISH + "." + key;
        }
        List<String> configured = getConfig().getStringList(path);
        return configured.isEmpty() ? fallback : configured;
    }

    private boolean languageSelectorEnabled() {
        return getConfig().getBoolean("language-selector.enabled",
                getConfig().getBoolean("mode-selector.enabled", false));
    }

    private String message(String language, String key) {
        String normalized = LanguageCode.isSupported(language) ? language : ENGLISH;
        String fallback = getConfig().getString("messages." + ENGLISH + "." + key, "");
        return getConfig().getString("messages." + normalized + "." + key, fallback);
    }


    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }



}

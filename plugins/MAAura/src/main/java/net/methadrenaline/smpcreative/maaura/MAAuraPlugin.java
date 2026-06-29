package net.methadrenaline.smpcreative.maaura;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import net.methadrenaline.smpcreative.maaura.aura.AuraCatalog;
import net.methadrenaline.smpcreative.maaura.lang.AuraMessages;
import net.methadrenaline.smpcreative.maaura.menu.AuraItemFactory;
import net.methadrenaline.smpcreative.maaura.menu.AuraMenuHolder;
import net.methadrenaline.smpcreative.maaura.menu.AuraMenuType;
import net.methadrenaline.smpcreative.maaura.particle.AuraParticleTask;
import net.methadrenaline.smpcreative.maaura.redeem.AuraToastService;
import net.methadrenaline.smpcreative.maaura.redeem.RedeemService;
import net.methadrenaline.smpcreative.maaura.storage.AuraStorage;
import net.methadrenaline.smpcreative.maaura.util.ServerId;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;

public final class MAAuraPlugin extends JavaPlugin implements Listener, TabExecutor {
    private static final int BACK_SLOT = 18;
    private static final int MAIN_SETTINGS_SLOT = 11;
    private static final int MAIN_CATALOG_SLOT = 13;
    private static final int MAIN_REDEEM_SLOT = 15;
    private static final int MAIN_INFO_SLOT = 31;
    private static final int LEFT_BUTTON_SLOT = 11;
    private static final int CENTER_BUTTON_SLOT = 13;
    private static final int RIGHT_BUTTON_SLOT = 15;
    private static final int CATALOG_FIND_SLOT = 12;
    private static final int CATALOG_LOYALTY_SLOT = 14;
    private static final int VISIBILITY_LEFT_SLOT = 11;
    private static final int VISIBILITY_CENTER_SLOT = 13;
    private static final int VISIBILITY_RIGHT_SLOT = 15;
    private static final String BACK_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWI2ZTIxOTFmMTRjZTk5MzJjM2UyNGM1ZjhjODQ3OWM3NDBjZjRkZmFmYTI1ODE1M2VlODA3MzYyYWY0ODEyIn19fQ==";
    private static final String REDEEM_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWQ5MDgxNjkzNjU2OTI5M2EzODI0ZTZmMWU3ZWEzNGE1NTJmZTkwMjY0YjAzNTJkOTE0ZjhmNjQ1NTI4YjkzZCJ9fX0=";

    private final AuraItemFactory itemFactory = new AuraItemFactory();
    private final AuraMessages messages = new AuraMessages(this);
    private final AuraToastService toastService = new AuraToastService(this, messages);
    private final AuraStorage auraStorage = new AuraStorage(this);
    private final RedeemService redeemService = new RedeemService(this, auraStorage);
    private NamespacedKey auraItemKey;
    private String serverId;
    private int particleTaskId = -1;
    private int lobbyItemTaskId = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureDefaultConfigValues();
        auraItemKey = new NamespacedKey(this, "aura_menu_item");
        serverId = ServerId.detect(this);
        auraStorage.load();

        Bukkit.getPluginManager().registerEvents(this, this);
        PluginCommand command = getCommand("aura");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }

        long interval = Math.max(1L, getConfig().getLong("settings.particle-interval-ticks", 10L));
        particleTaskId = Bukkit.getScheduler().runTaskTimer(this, new AuraParticleTask(this), 20L, interval).getTaskId();

        if ("lobby".equals(serverId)) {
            long refreshInterval = Math.max(10L, getConfig().getLong("settings.lobby-item-refresh-ticks", 40L));
            lobbyItemTaskId = Bukkit.getScheduler().runTaskTimer(this, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    giveLobbyItem(player);
                }
            }, refreshInterval, refreshInterval).getTaskId();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            scheduleLobbyItem(player, 10);
        }
        getLogger().info("MAAura loaded for server id: " + serverId);
    }

    private void ensureDefaultConfigValues() {
        boolean changed = false;
        changed |= setDefaultConfigValue("settings.main-menu-size", 45);
        changed |= setDefaultConfigValue("auras." + AuraCatalog.LOYALTY + ".owners", List.of());
        if ("GLOW_BERRIES".equalsIgnoreCase(getConfig().getString("auras." + AuraCatalog.LOYALTY + ".material", ""))) {
            getConfig().set("auras." + AuraCatalog.LOYALTY + ".material", "EMERALD");
            changed = true;
        }
        changed |= setDefaultConfigValue("auras." + AuraCatalog.LOYALTY + ".material", "EMERALD");
        changed |= setDefaultConfigValue("auras." + AuraCatalog.LOYALTY + ".particle", "minecraft:glow");
        changed |= setDefaultConfigValue("auras." + AuraCatalog.LOYALTY + ".offset-x", 1);
        changed |= setDefaultConfigValue("auras." + AuraCatalog.LOYALTY + ".offset-y", 1.5);
        changed |= setDefaultConfigValue("auras." + AuraCatalog.LOYALTY + ".offset-z", 0.5);
        changed |= setDefaultConfigValue("auras." + AuraCatalog.LOYALTY + ".speed", 10);
        changed |= setDefaultConfigValue("auras." + AuraCatalog.LOYALTY + ".count", 1);
        changed |= setDefaultConfigValue("auras." + AuraCatalog.LOYALTY + ".mode", "force");
        changed |= clearConfigValue("redeem.input-window.sign-material");
        changed |= clearConfigValue("redeem.input-window.input-line");
        changed |= clearConfigValue("redeem.input-window.y-offset-below-player");
        changed |= clearConfigValue("redeem.input-window.restore-delay-ticks");
        changed |= clearConfigValue("redeem.input-window.lines");
        changed |= setLocalizedConfigValueIfMissingOrLegacy("redeem.input-window.title",
                Map.of(AuraMessages.ENGLISH, "&fRedeem your aura code", AuraMessages.UKRAINIAN, "&fАктивуй код аури", AuraMessages.RUSSIAN, "&fАктивируй код ауры"),
                Set.of("&aRedeem", "&aАктивувати", "&aАктивировать"));
        changed |= setLocalizedConfigValueIfMissingOrLegacy("redeem.input-window.hint",
                Map.of(AuraMessages.ENGLISH, "&7XXXXX-XXXXX-XXXXX-XXXXX-XXXXX", AuraMessages.UKRAINIAN, "&7XXXXX-XXXXX-XXXXX-XXXXX-XXXXX", AuraMessages.RUSSIAN, "&7XXXXX-XXXXX-XXXXX-XXXXX-XXXXX"),
                Set.of("&7Enter code", "&7Введи код"));
        changed |= setDefaultLocalizedConfigValue("redeem.input-window.explanation",
                Map.of(
                        AuraMessages.ENGLISH, "&7You can receive an aura activation code for taking part in events, or from a friend who shared the code with you. After activating a valid code, you will receive your aura! One code can be activated only once, and after activation it immediately becomes invalid.",
                        AuraMessages.UKRAINIAN, "&7Код активації аури тобі можуть дати за участь у подіях або твій друг, який поділився з тобою цим кодом. Після активації дійсного коду ти отримаєш свою ауру! Один код можна активувати лише один раз, після активації він одразу втрачає актуальність.",
                        AuraMessages.RUSSIAN, "&7Код активации ауры тебе могут дать за участие в событиях или твой друг, который поделился с тобой этим кодом. После активации действительного кода ты получишь свою ауру! Один код можно активировать только один раз, после активации он сразу теряет актуальность."));
        changed |= setDefaultConfigValue("redeem.input-window.input-key", "code");
        changed |= setDefaultConfigValue("redeem.input-window.input-width", 300);
        changed |= setDefaultConfigValue("redeem.input-window.label-visible", true);
        changed |= setDefaultConfigValue("redeem.input-window.initial", "");
        changed |= setDefaultConfigValue("redeem.input-window.max-length", 29);
        changed |= setDefaultLocalizedConfigValue("redeem.input-window.submit-label",
                Map.of(AuraMessages.ENGLISH, "&aRedeem", AuraMessages.UKRAINIAN, "&a\u0410\u043A\u0442\u0438\u0432\u0443\u0432\u0430\u0442\u0438", AuraMessages.RUSSIAN, "&a\u0410\u043A\u0442\u0438\u0432\u0438\u0440\u043E\u0432\u0430\u0442\u044C"));
        changed |= setDefaultLocalizedConfigValue("redeem.input-window.back-label",
                Map.of(AuraMessages.ENGLISH, "&fBack", AuraMessages.UKRAINIAN, "&fНазад", AuraMessages.RUSSIAN, "&fНазад"));
        changed |= setDefaultConfigValue("redeem.input-window.submit-width", 120);
        changed |= setDefaultConfigValue("redeem.input-window.explanation-width", 320);
        changed |= setDefaultConfigValue("redeem.input-window.columns", 1);
        changed |= setDefaultConfigValue("redeem.input-window.callback-lifetime-seconds", 300);
        changed |= setDefaultConfigValue("redeem.input-window.can-close-with-escape", true);
        changed |= setDefaultConfigValue("redeem.input-window.pause", false);
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

    private boolean setDefaultLocalizedConfigValue(String path, Map<String, String> values) {
        boolean changed = false;
        if (!getConfig().isConfigurationSection(path)) {
            getConfig().set(path, null);
            changed = true;
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            changed |= setDefaultConfigValue(path + "." + entry.getKey(), entry.getValue());
        }
        return changed;
    }

    private boolean setLocalizedConfigValueIfMissingOrLegacy(String path, Map<String, String> values, Set<String> legacyValues) {
        boolean changed = false;
        if (!getConfig().isConfigurationSection(path)) {
            getConfig().set(path, null);
            changed = true;
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = path + "." + entry.getKey();
            String existing = getConfig().getString(key);
            if (existing == null || existing.isBlank() || legacyValues.contains(existing)) {
                getConfig().set(key, entry.getValue());
                changed = true;
            }
        }
        return changed;
    }

    private boolean clearConfigValue(String path) {
        if (!getConfig().isSet(path)) {
            return false;
        }
        getConfig().set(path, null);
        return true;
    }

    @Override
    public void onDisable() {
        if (particleTaskId != -1) {
            Bukkit.getScheduler().cancelTask(particleTaskId);
            particleTaskId = -1;
        }
        if (lobbyItemTaskId != -1) {
            Bukkit.getScheduler().cancelTask(lobbyItemTaskId);
            lobbyItemTaskId = -1;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /" + label + ".");
            return true;
        }

        openMainMenu(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduleLobbyItem(event.getPlayer(), 20);
    }

    @EventHandler
    public void onLocaleChange(PlayerLocaleChangeEvent event) {
        scheduleLobbyItem(event.getPlayer(), 2);
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase(Locale.ROOT);
        if (message.equals("/lang") || message.startsWith("/lang ") || message.equals("/language") || message.startsWith("/language ")) {
            scheduleLobbyItem(event.getPlayer(), 2);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!isAuraItem(event.getItem())) {
            return;
        }

        event.setCancelled(true);
        openMainMenu(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof AuraMenuHolder holder && event.getWhoClicked() instanceof Player player) {
            event.setCancelled(true);
            handleMenuClick(player, holder.type(), event.getRawSlot());
            return;
        }

        if (isAuraItem(event.getCurrentItem()) || isAuraItem(event.getCursor())
                || isAuraItem(event.getHotbarButton() >= 0 ? event.getWhoClicked().getInventory().getItem(event.getHotbarButton()) : null)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof AuraMenuHolder || isAuraItem(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (isAuraItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (isAuraItem(event.getMainHandItem()) || isAuraItem(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    private void handleMenuClick(Player player, AuraMenuType type, int slot) {
        if (slot < 0 || slot >= menuSize(type)) {
            return;
        }

        switch (type) {
            case MAIN -> {
                if (slot == MAIN_CATALOG_SLOT) {
                    openCatalogMenu(player);
                } else if (slot == MAIN_SETTINGS_SLOT) {
                    openSettingsMenu(player);
                } else if (slot == MAIN_REDEEM_SLOT) {
                    openRedeemMenu(player);
                }
            }
            case CATALOG -> {
                if (slot == BACK_SLOT) {
                    openMainMenu(player);
                } else if (slot == CATALOG_FIND_SLOT) {
                    toggleAura(player, AuraCatalog.FIND_ZEN);
                    openCatalogMenu(player);
                } else if (slot == CATALOG_LOYALTY_SLOT) {
                    toggleAura(player, AuraCatalog.LOYALTY);
                    openCatalogMenu(player);
                }
            }
            case SETTINGS -> {
                if (slot == BACK_SLOT) {
                    openMainMenu(player);
                } else if (slot == LEFT_BUTTON_SLOT) {
                    togglePrivacy(player);
                    openSettingsMenu(player);
                } else if (slot == RIGHT_BUTTON_SLOT) {
                    openVisibilityMenu(player);
                }
            }
            case VISIBILITY -> {
                if (slot == BACK_SLOT) {
                    openSettingsMenu(player);
                } else if (slot == VISIBILITY_LEFT_SLOT) {
                    auraStorage.setShowOthers(player, !auraStorage.showOthers(player));
                    openVisibilityMenu(player);
                } else if (slot == VISIBILITY_CENTER_SLOT) {
                    auraStorage.setShowOwn(player, !auraStorage.showOwn(player));
                    openVisibilityMenu(player);
                } else if (slot == VISIBILITY_RIGHT_SLOT) {
                    cycleScope(player);
                    openVisibilityMenu(player);
                }
            }
        }
    }

    private void openMainMenu(Player player) {
        Inventory inventory = menu(player, AuraMenuType.MAIN, messages.text(player, "menu-title"));
        inventory.setItem(MAIN_CATALOG_SLOT, simpleItem(Material.PAPER, "&6" + messages.text(player, "catalog"), List.of()));
        inventory.setItem(MAIN_SETTINGS_SLOT, simpleItem(Material.COMPARATOR, "&6" + messages.text(player, "settings"), List.of()));
        inventory.setItem(MAIN_REDEEM_SLOT, simpleItem(itemFactory.customHead(REDEEM_TEXTURE), "&a" + messages.text(player, "redeem"),
                List.of("&7" + messages.text(player, "redeem-desc"))));
        inventory.setItem(MAIN_INFO_SLOT, simpleItem(Material.AMETHYST_CLUSTER, "&d" + messages.text(player, "aura-info"), messages.auraInfoLore(player)));
        player.openInventory(inventory);
    }

    private void openCatalogMenu(Player player) {
        Inventory inventory = menu(player, AuraMenuType.CATALOG, messages.text(player, "catalog-title"));
        addBackButton(inventory, player);
        inventory.setItem(CATALOG_FIND_SLOT, auraCatalogItem(player, AuraCatalog.FIND_ZEN));
        inventory.setItem(CATALOG_LOYALTY_SLOT, auraCatalogItem(player, AuraCatalog.LOYALTY));
        player.openInventory(inventory);
    }

    private void openRedeemMenu(Player player) {
        player.closeInventory();
        try {
            ((Audience) player).showDialog(redeemDialog(player));
        } catch (RuntimeException exception) {
            player.sendMessage(color("&c" + messages.text(player, "redeem-invalid")));
            getLogger().warning("Could not open redeem dialog for " + player.getName() + ": " + exception.getMessage());
        }
    }

    private Dialog redeemDialog(Player player) {
        String inputKey = getConfig().getString("redeem.input-window.input-key", "code");
        if (inputKey == null || inputKey.isBlank()) {
            inputKey = "code";
        }

        Component title = itemFactory.itemText(localizedConfigText(player, "redeem.input-window.title", "&a" + messages.text(player, "redeem")));
        Component explanation = itemFactory.itemText(localizedConfigText(player, "redeem.input-window.explanation", ""));
        DialogInput input = DialogInput.text(inputKey, itemFactory.itemText(localizedConfigText(player, "redeem.input-window.hint", "&7" + messages.text(player, "redeem-hint"))))
                .width(Math.max(80, getConfig().getInt("redeem.input-window.input-width", 300)))
                .labelVisible(getConfig().getBoolean("redeem.input-window.label-visible", true))
                .initial(getConfig().getString("redeem.input-window.initial", ""))
                .maxLength(Math.max(1, getConfig().getInt("redeem.input-window.max-length", 29)))
                .build();

        String callbackInputKey = inputKey;
        ActionButton submitButton = ActionButton.builder(itemFactory.itemText(localizedConfigText(player, "redeem.input-window.submit-label", "&a" + messages.text(player, "redeem"))))
                .width(Math.max(40, getConfig().getInt("redeem.input-window.submit-width", 120)))
                .action(DialogAction.customClick((response, audience) -> handleRedeemDialogResponse(response.getText(callbackInputKey), audience),
                        ClickCallback.Options.builder()
                                .uses(1)
                                .lifetime(Duration.ofSeconds(Math.max(5, getConfig().getLong("redeem.input-window.callback-lifetime-seconds", 300L))))
                                .build()))
                .build();
        ActionButton backButton = ActionButton.builder(itemFactory.itemText(localizedConfigText(player, "redeem.input-window.back-label", "&fBack")))
                .width(Math.max(40, getConfig().getInt("redeem.input-window.submit-width", 120)))
                .action(DialogAction.customClick((response, audience) -> handleRedeemBack(audience),
                        ClickCallback.Options.builder()
                                .uses(1)
                                .lifetime(Duration.ofSeconds(Math.max(5, getConfig().getLong("redeem.input-window.callback-lifetime-seconds", 300L))))
                                .build()))
                .build();

        return Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(title)
                        .externalTitle(title)
                        .canCloseWithEscape(getConfig().getBoolean("redeem.input-window.can-close-with-escape", true))
                        .pause(getConfig().getBoolean("redeem.input-window.pause", false))
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(List.of(DialogBody.plainMessage(explanation, Math.max(120, getConfig().getInt("redeem.input-window.explanation-width", 320)))))
                        .inputs(List.of(input))
                        .build())
                .type(DialogType.multiAction(List.of(submitButton, backButton))
                        .columns(Math.max(1, getConfig().getInt("redeem.input-window.columns", 1)))
                        .build()));
    }

    private void handleRedeemBack(Audience audience) {
        if (!(audience instanceof Player player)) {
            return;
        }
        Bukkit.getScheduler().runTask(this, () -> {
            if (player.isOnline()) {
                openMainMenu(player);
            }
        });
    }

    private void handleRedeemDialogResponse(String rawCode, Audience audience) {
        if (!(audience instanceof Player player)) {
            return;
        }
        Bukkit.getScheduler().runTask(this, () -> {
            if (player.isOnline()) {
                submitRedeemCode(player, rawCode);
            }
        });
    }

    private String localizedConfigText(Player player, String path, String fallback) {
        String language = messages.languageFor(player);
        if (getConfig().isConfigurationSection(path)) {
            String localized = getConfig().getString(path + "." + language);
            if (localized == null || localized.isBlank()) {
                localized = getConfig().getString(path + "." + AuraMessages.ENGLISH);
            }
            return localized == null || localized.isBlank() ? fallback : localized;
        }

        String legacyValue = getConfig().getString(path);
        return legacyValue == null || legacyValue.isBlank() ? fallback : legacyValue;
    }

    private void openSettingsMenu(Player player) {
        Inventory inventory = menu(player, AuraMenuType.SETTINGS, messages.text(player, "settings-title"));
        addBackButton(inventory, player);
        String privacy = auraStorage.privacy(player);
        List<String> privacyLore = new ArrayList<>();
        privacyLore.add("&7" + messages.text(player, "mode") + ": " + ("private".equals(privacy) ? "&9" + messages.text(player, "private") : "&b" + messages.text(player, "public")));
        privacyLore.add("&7" + ("private".equals(privacy) ? messages.text(player, "private-desc") : messages.text(player, "public-desc")));
        inventory.setItem(LEFT_BUTTON_SLOT, simpleItem(Material.ENDER_EYE, "&6" + messages.text(player, "privacy"), privacyLore));

        List<String> visibilityLore = List.of("&7" + messages.text(player, "visibility-desc"));
        inventory.setItem(RIGHT_BUTTON_SLOT, simpleItem(Material.SPYGLASS, "&6" + messages.text(player, "visibility"), visibilityLore));
        player.openInventory(inventory);
    }

    private void openVisibilityMenu(Player player) {
        Inventory inventory = menu(player, AuraMenuType.VISIBILITY, messages.text(player, "visibility-title"));
        addBackButton(inventory, player);

        inventory.setItem(VISIBILITY_LEFT_SLOT, toggleItem(Material.PLAYER_HEAD, "&6" + messages.text(player, "others-aura"),
                auraStorage.showOthers(player), List.of("&7" + messages.text(player, "others-desc")), player));
        inventory.setItem(VISIBILITY_CENTER_SLOT, toggleItem(Material.AMETHYST_SHARD, "&6" + messages.text(player, "own-aura"),
                auraStorage.showOwn(player), List.of("&7" + messages.text(player, "own-desc")), player));

        List<String> scopeLore = List.of(
                "&7" + messages.text(player, "mode") + ": " + messages.scopeLabel(player, auraStorage.scope(player)),
                "&7" + messages.scopeDescription(player, auraStorage.scope(player))
        );
        inventory.setItem(VISIBILITY_RIGHT_SLOT, simpleItem(Material.RECOVERY_COMPASS, "&6" + messages.text(player, "scope"), scopeLore));
        player.openInventory(inventory);
    }

    private Inventory menu(Player player, AuraMenuType type, String title) {
        Inventory inventory = Bukkit.createInventory(new AuraMenuHolder(type), menuSize(type), color(title));
        ItemStack filler = simpleItem(AuraItemFactory.material(getConfig().getString("settings.filler-material", "GRAY_STAINED_GLASS_PANE"), Material.GRAY_STAINED_GLASS_PANE), " ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
        return inventory;
    }

    private void addBackButton(Inventory inventory, Player player) {
        inventory.setItem(BACK_SLOT, simpleItem(itemFactory.customHead(BACK_TEXTURE), "&6" + messages.text(player, "back"), List.of()));
    }

    private ItemStack auraCatalogItem(Player player, String auraId) {
        boolean owner = auraStorage.ownsAura(player, auraId);
        boolean enabled = auraStorage.auraEnabled(player, auraId);

        List<String> lore = new ArrayList<>();
        lore.add("&7" + messages.text(player, "owners") + ": &f" + auraStorage.auraOwners(auraId).size());
        if (owner) {
            lore.add(enabled ? "&a" + messages.text(player, "enabled") : "&c" + messages.text(player, "disabled"));
        } else {
            lore.add("&c" + messages.text(player, "not-owner"));
        }

        ItemStack item = simpleItem(AuraItemFactory.material(getConfig().getString("auras." + auraId + ".material", "CHERRY_SAPLING"), Material.CHERRY_SAPLING),
                "&d" + messages.auraName(player, auraId), lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null && owner && enabled) {
            itemFactory.setGlint(meta, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack toggleItem(Material material, String name, boolean enabled, List<String> lore, Player player) {
        List<String> fullLore = new ArrayList<>(lore);
        fullLore.add(enabled ? "&a" + messages.text(player, "enabled") : "&c" + messages.text(player, "disabled"));
        ItemStack item = simpleItem(material, name, fullLore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null && enabled) {
            itemFactory.setGlint(meta, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack simpleItem(Material material, String name, List<String> lore) {
        return simpleItem(new ItemStack(material), name, lore);
    }

    private ItemStack simpleItem(ItemStack item, String name, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            itemFactory.applyItemText(meta, name, lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void scheduleLobbyItem(Player player, int delayTicks) {
        if (!"lobby".equals(serverId)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                giveLobbyItem(player);
            }
        }, Math.max(1, delayTicks));
    }

    public void refreshLobbyItem(Player player) {
        scheduleLobbyItem(player, 1);
    }

    private void giveLobbyItem(Player player) {
        int slot = Math.max(0, Math.min(8, getConfig().getInt("settings.lobby-item-slot", 0)));
        clearExtraAuraItems(player, slot);

        ItemStack item = new ItemStack(Material.AMETHYST_CLUSTER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            itemFactory.applyItemText(meta, "&d" + messages.text(player, "aura-button"), List.of("&7" + messages.text(player, "aura-button-desc")));
            meta.getPersistentDataContainer().set(auraItemKey, PersistentDataType.BYTE, (byte) 1);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        player.getInventory().setItem(slot, item);
    }

    private void clearExtraAuraItems(Player player, int targetSlot) {
        for (int slot = 0; slot <= 8; slot++) {
            if (slot != targetSlot && isAuraItem(player.getInventory().getItem(slot))) {
                player.getInventory().setItem(slot, null);
            }
        }
    }

    private boolean isAuraItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(auraItemKey, PersistentDataType.BYTE);
    }

    public void tickAuras() {
        auraStorage.loadIfChanged();
        for (String auraId : auraIds()) {
            Particle particle = particle(auraId);
            for (Player owner : Bukkit.getOnlinePlayers()) {
                if (!auraStorage.ownsAura(owner, auraId) || !auraStorage.auraEnabled(owner, auraId) || !scopeAllows(owner)) {
                    continue;
                }

                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    if (viewer.getUniqueId().equals(owner.getUniqueId())) {
                        if (auraStorage.showOwn(owner)) {
                            spawnAuraParticle(viewer, owner, auraId, particle);
                        }
                        continue;
                    }

                    if ("public".equals(auraStorage.privacy(owner)) && auraStorage.showOthers(viewer)) {
                        spawnAuraParticle(viewer, owner, auraId, particle);
                    }
                }
            }
        }
    }

    private void spawnAuraParticle(Player viewer, Player owner, String auraId, Particle particle) {
        Location location = owner.getLocation();
        int count = Math.max(1, getConfig().getInt("auras." + auraId + ".count",
                getConfig().getInt("auras." + auraId + ".particle-count", 1)));
        viewer.spawnParticle(
                particle,
                location,
                count,
                getConfig().getDouble("auras." + auraId + ".offset-x", 1.0),
                getConfig().getDouble("auras." + auraId + ".offset-y", 1.5),
                getConfig().getDouble("auras." + auraId + ".offset-z", 0.5),
                getConfig().getDouble("auras." + auraId + ".speed", 10.0)
        );
    }

    private void toggleAura(Player player, String auraId) {
        if (!auraStorage.ownsAura(player, auraId)) {
            player.sendMessage(color("&c" + messages.text(player, "not-owner")));
            return;
        }

        if (auraStorage.auraEnabled(player, auraId)) {
            auraStorage.setAuraEnabled(player, auraId, false);
            return;
        }

        auraStorage.setExclusiveAuraEnabled(player, auraId);
    }

    private void submitRedeemCode(Player player, String rawCode) {
        String auraId = redeemService.redeem(player, rawCode);
        if (auraId == null) {
            player.sendMessage(color("&c" + messages.text(player, "redeem-invalid")));
            return;
        }

        player.closeInventory();
        toastService.show(player, auraId);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                openCatalogMenu(player);
            }
        }, 10L);
    }

    private void togglePrivacy(Player player) {
        auraStorage.setPrivacy(player, "private".equals(auraStorage.privacy(player)) ? "public" : "private");
    }

    private void cycleScope(Player player) {
        String next = switch (auraStorage.scope(player)) {
            case "lobby" -> "survival";
            case "survival" -> "creative";
            case "creative" -> "all";
            default -> "lobby";
        };
        auraStorage.setScope(player, next);
    }

    private boolean scopeAllows(Player player) {
        String scope = auraStorage.scope(player);
        return "all".equals(scope) || serverId.equals(scope);
    }

    private int menuSize() {
        int size = Math.max(9, Math.min(54, getConfig().getInt("settings.menu-size", 27)));
        return ((size + 8) / 9) * 9;
    }

    private int menuSize(AuraMenuType type) {
        if (type == AuraMenuType.MAIN) {
            int size = Math.max(45, Math.min(54, getConfig().getInt("settings.main-menu-size", 45)));
            return ((size + 8) / 9) * 9;
        }
        return menuSize();
    }

    private List<String> auraIds() {
        return AuraCatalog.ids(getConfig());
    }

    private Particle particle(String auraId) {
        String configured = getConfig().getString("auras." + auraId + ".particle", "minecraft:cherry_leaves");
        String enumName = configured == null ? "CHERRY_LEAVES" : configured.replace("minecraft:", "")
                .replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return Particle.valueOf(enumName);
        } catch (IllegalArgumentException exception) {
            return Particle.valueOf("HAPPY_VILLAGER");
        }
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

}

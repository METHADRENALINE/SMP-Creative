package net.methadrenaline.smpcreative.madialogs;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.methadrenaline.smpcreative.madialogs.dialog.DialogRegistry;
import net.methadrenaline.smpcreative.madialogs.dialog.DialogVariant;
import net.methadrenaline.smpcreative.madialogs.dialog.NpcDialog;
import net.methadrenaline.smpcreative.madialogs.lang.LanguageResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class MADialogsPlugin extends JavaPlugin implements TabExecutor {
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
    private final DialogRegistry registry = new DialogRegistry();

    @Override
    public void onEnable() {
        saveDefaultDialogs();
        reloadDialogs();

        var command = getCommand("madialog");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /" + label + " <dialog>");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("madialogs.admin")) {
                sender.sendMessage("You do not have permission to reload MADialogs.");
                return true;
            }

            reloadDialogs();
            sender.sendMessage("MADialogs reloaded. Loaded dialogs: " + registry.size());
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can open NPC dialogs.");
            return true;
        }

        openDialog(player, args[0]);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }

        List<String> options = new ArrayList<>(registry.ids());
        if (sender.hasPermission("madialogs.admin")) {
            options.add("reload");
        }
        options.removeIf(option -> !option.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)));
        Collections.sort(options);
        return options;
    }

    private void openDialog(Player player, String dialogId) {
        NpcDialog npcDialog = registry.get(dialogId);
        if (npcDialog == null) {
            player.sendMessage(Component.text("Unknown NPC dialog: " + dialogId, NamedTextColor.RED));
            return;
        }

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        if (!(book.getItemMeta() instanceof BookMeta meta)) {
            return;
        }

        DialogVariant variant = npcDialog.variant(LanguageResolver.resolve(player, getLogger()));
        meta.title(Component.text(variant.title()));
        meta.author(Component.text(registry.settings().author()));
        for (String page : variant.pages()) {
            meta.addPages(legacy.deserialize(page));
        }

        book.setItemMeta(meta);
        player.openBook(book);
        playOpenSounds(player, npcDialog);
    }

    private void playOpenSounds(Player player, NpcDialog npcDialog) {
        List<String> sounds = npcDialog.pageSounds().getOrDefault(1, List.of());
        for (int i = 0; i < sounds.size(); i++) {
            String sound = sounds.get(i);
            long delay = (long) i * registry.settings().soundDelayTicks();
            Bukkit.getScheduler().runTaskLater(this, () ->
                    player.playSound(player.getLocation(), sound, SoundCategory.VOICE, 1.0F, 1.0F), delay);
        }
    }

    private void reloadDialogs() {
        registry.reload(new File(getDataFolder(), "dialogs.yml"), getLogger());
    }

    private void saveDefaultDialogs() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Could not create plugin data folder.");
        }

        File dialogsFile = new File(getDataFolder(), "dialogs.yml");
        if (!dialogsFile.exists()) {
            saveResource("dialogs.yml", false);
        }
    }
}

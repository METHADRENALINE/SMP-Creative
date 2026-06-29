package net.methadrenaline.smpcreative.macore.teleport;

import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class TeleportCommand implements TabExecutor {
    private final TeleportRequestService service;

    public TeleportCommand(TeleportRequestService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /" + label + ".");
            return true;
        }

        if (!service.isTeleportServer()) {
            service.send(player, "only-modes");
            return true;
        }

        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "tpa" -> service.requestTeleport(player, args);
            case "tpyes" -> service.answer(player, args, true);
            case "tpno" -> service.answer(player, args, false);
            default -> true;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || args.length != 1 || !service.isTeleportServer()) {
            return List.of();
        }

        return service.suggestions(player, command.getName(), args[0]);
    }
}

package net.methadrenaline.smpcreative.mavelocore.whisper;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class WhisperCommand implements SimpleCommand {
    private final ProxyServer server;

    public WhisperCommand(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player sender)) {
            source.sendMessage(Component.text("Only players can use /msg.", NamedTextColor.RED));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /msg <player> <message>", NamedTextColor.RED));
            return;
        }

        Optional<Player> targetOptional = findPlayer(args[0]);
        if (targetOptional.isEmpty()) {
            sender.sendMessage(Component.translatable("argument.entity.notfound.player", Component.text(args[0]))
                    .color(NamedTextColor.RED));
            return;
        }

        sendVanillaWhisper(sender, targetOptional.get(), joinArgs(args, 1));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length > 1) {
            return List.of();
        }

        String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        for (Player player : server.getAllPlayers()) {
            String name = player.getUsername();
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                suggestions.add(name);
            }
        }
        return suggestions;
    }

    private Optional<Player> findPlayer(String name) {
        Optional<Player> exact = server.getPlayer(name);
        if (exact.isPresent()) {
            return exact;
        }

        String normalized = name.toLowerCase(Locale.ROOT);
        Player match = null;
        for (Player player : server.getAllPlayers()) {
            if (!player.getUsername().toLowerCase(Locale.ROOT).startsWith(normalized)) {
                continue;
            }
            if (match != null) {
                return Optional.empty();
            }
            match = player;
        }
        return Optional.ofNullable(match);
    }

    private void sendVanillaWhisper(Player sender, Player target, String message) {
        Component messageComponent = Component.text(message);
        Component incoming = Component.translatable(
                        "commands.message.display.incoming",
                        Component.text(sender.getUsername()),
                        messageComponent
                )
                .color(NamedTextColor.GRAY)
                .decorate(TextDecoration.ITALIC);
        Component outgoing = Component.translatable(
                        "commands.message.display.outgoing",
                        Component.text(target.getUsername()),
                        messageComponent
                )
                .color(NamedTextColor.GRAY)
                .decorate(TextDecoration.ITALIC);

        target.sendMessage(incoming);
        sender.sendMessage(outgoing);
    }

    private static String joinArgs(String[] args, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }
}

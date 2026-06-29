package net.methadrenaline.smpcreative.macore.teleport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.methadrenaline.smpcreative.macore.lang.CoreMessages;
import net.methadrenaline.smpcreative.macore.lang.LanguageProvider;
import net.methadrenaline.smpcreative.macore.util.ServerId;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class TeleportRequestService implements Listener {
    private final JavaPlugin plugin;
    private final CoreMessages messages;
    private final String serverId;
    private final Map<UUID, Map<UUID, TeleportRequest>> requestsByTarget = new HashMap<>();
    private final Map<UUID, UUID> latestRequesterByTarget = new HashMap<>();

    public TeleportRequestService(JavaPlugin plugin, CoreMessages messages, String serverId) {
        this.plugin = plugin;
        this.messages = messages;
        this.serverId = serverId;
    }

    public boolean isTeleportServer() {
        List<String> allowed = plugin.getConfig().getStringList("server.teleport-servers").stream()
                .map(ServerId::normalize)
                .toList();
        return allowed.contains(serverId);
    }

    public void send(Player player, String key) {
        player.sendMessage(messages.component(player, key));
    }

    public List<String> suggestions(Player player, String commandName, String prefix) {
        String normalizedCommand = commandName.toLowerCase(Locale.ROOT);
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        if ("tpa".equals(normalizedCommand)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.getUniqueId().equals(player.getUniqueId())
                        && online.getName().toLowerCase(Locale.ROOT).startsWith(normalizedPrefix)) {
                    options.add(online.getName());
                }
            }
            return options;
        }

        Map<UUID, TeleportRequest> requests = requestsByTarget.get(player.getUniqueId());
        if (requests == null) {
            return List.of();
        }

        for (UUID requesterId : requests.keySet()) {
            Player requester = Bukkit.getPlayer(requesterId);
            if (requester != null && requester.getName().toLowerCase(Locale.ROOT).startsWith(normalizedPrefix)) {
                options.add(requester.getName());
            }
        }
        return options;
    }

    public boolean requestTeleport(Player requester, String[] args) {
        if (args.length != 1) {
            send(requester, "tpa-usage");
            return true;
        }

        Player target = findPlayer(args[0]);
        if (target == null) {
            requester.sendMessage(messages.component(requester, "player-not-found").replaceText(builder ->
                    builder.matchLiteral("%player%").replacement(args[0])));
            return true;
        }

        if (target.getUniqueId().equals(requester.getUniqueId())) {
            send(requester, "self-request");
            return true;
        }

        int timeoutSeconds = Math.max(1, plugin.getConfig().getInt("teleport.timeout-seconds", 120));
        Map<UUID, TeleportRequest> byRequester = requestsByTarget.computeIfAbsent(
                target.getUniqueId(), ignored -> new HashMap<>());
        TeleportRequest previous = byRequester.remove(requester.getUniqueId());
        if (previous != null) {
            Bukkit.getScheduler().cancelTask(previous.expireTaskId());
        }

        int taskId = Bukkit.getScheduler().runTaskLater(plugin,
                () -> expireRequest(requester.getUniqueId(), target.getUniqueId()),
                timeoutSeconds * 20L).getTaskId();
        byRequester.put(requester.getUniqueId(), new TeleportRequest(
                requester.getUniqueId(),
                target.getUniqueId(),
                System.currentTimeMillis() + timeoutSeconds * 1000L,
                taskId
        ));
        latestRequesterByTarget.put(target.getUniqueId(), requester.getUniqueId());

        requester.sendMessage(messages.component(requester, "request-sent").replaceText(builder ->
                builder.matchLiteral("%player%").replacement(target.getName())));
        target.sendMessage(messages.component(target, "request-received").replaceText(builder ->
                builder.matchLiteral("%player%").replacement(requester.getName())));
        target.sendMessage(buttons(target, requester));
        target.sendMessage(messages.component(target, "timeout").replaceText(builder ->
                builder.matchLiteral("%seconds%").replacement(String.valueOf(timeoutSeconds))));
        return true;
    }

    public boolean answer(Player target, String[] args, boolean accept) {
        TeleportRequest request = requestFor(target, args);
        if (request == null) {
            send(target, "no-request");
            return true;
        }

        removeRequest(request);
        Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester == null) {
            send(target, "requester-offline");
            return true;
        }

        if (!accept) {
            target.sendMessage(messages.component(target, "denied-target").replaceText(builder ->
                    builder.matchLiteral("%player%").replacement(requester.getName())));
            requester.sendMessage(messages.component(requester, "denied-requester").replaceText(builder ->
                    builder.matchLiteral("%player%").replacement(target.getName())));
            return true;
        }

        target.sendMessage(messages.component(target, "accepted-target").replaceText(builder ->
                builder.matchLiteral("%player%").replacement(requester.getName())));
        requester.sendMessage(messages.component(requester, "accepted-requester").replaceText(builder ->
                builder.matchLiteral("%player%").replacement(target.getName())));
        requester.teleportAsync(target.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND)
                .thenAccept(success -> {
                    if (!success) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (requester.isOnline()) {
                                send(requester, "teleport-failed");
                            }
                        });
                    }
                });
        return true;
    }

    public void cancelAll() {
        for (Map<UUID, TeleportRequest> byRequester : requestsByTarget.values()) {
            for (TeleportRequest request : byRequester.values()) {
                Bukkit.getScheduler().cancelTask(request.expireTaskId());
            }
        }
        requestsByTarget.clear();
        latestRequesterByTarget.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        Map<UUID, TeleportRequest> targetedRequests = requestsByTarget.remove(playerId);
        if (targetedRequests != null) {
            for (TeleportRequest request : targetedRequests.values()) {
                Bukkit.getScheduler().cancelTask(request.expireTaskId());
            }
        }
        latestRequesterByTarget.remove(playerId);

        for (Map.Entry<UUID, Map<UUID, TeleportRequest>> entry : new ArrayList<>(requestsByTarget.entrySet())) {
            TeleportRequest request = entry.getValue().remove(playerId);
            if (request != null) {
                Bukkit.getScheduler().cancelTask(request.expireTaskId());
            }
            if (entry.getValue().isEmpty()) {
                requestsByTarget.remove(entry.getKey());
                latestRequesterByTarget.remove(entry.getKey());
            } else if (playerId.equals(latestRequesterByTarget.get(entry.getKey()))) {
                latestRequesterByTarget.put(entry.getKey(), entry.getValue().keySet().iterator().next());
            }
        }
    }

    private TeleportRequest requestFor(Player target, String[] args) {
        Map<UUID, TeleportRequest> requests = requestsByTarget.get(target.getUniqueId());
        if (requests == null || requests.isEmpty()) {
            return null;
        }

        if (args.length > 0) {
            Player requester = findPlayer(args[0]);
            return requester == null ? null : requests.get(requester.getUniqueId());
        }

        UUID latestRequester = latestRequesterByTarget.get(target.getUniqueId());
        if (latestRequester != null && requests.containsKey(latestRequester)) {
            return requests.get(latestRequester);
        }

        return requests.values().iterator().next();
    }

    private void expireRequest(UUID requesterId, UUID targetId) {
        Map<UUID, TeleportRequest> requests = requestsByTarget.get(targetId);
        if (requests == null) {
            return;
        }

        TeleportRequest request = requests.remove(requesterId);
        if (request == null) {
            return;
        }

        if (requests.isEmpty()) {
            requestsByTarget.remove(targetId);
            latestRequesterByTarget.remove(targetId);
        } else if (requesterId.equals(latestRequesterByTarget.get(targetId))) {
            latestRequesterByTarget.put(targetId, requests.keySet().iterator().next());
        }

        Player requester = Bukkit.getPlayer(requesterId);
        Player target = Bukkit.getPlayer(targetId);
        if (requester != null) {
            requester.sendMessage(messages.component(requester, "expired-requester").replaceText(builder ->
                    builder.matchLiteral("%player%").replacement(target == null ? "player" : target.getName())));
        }
        if (target != null) {
            target.sendMessage(messages.component(target, "expired-target").replaceText(builder ->
                    builder.matchLiteral("%player%").replacement(requester == null ? "player" : requester.getName())));
        }
    }

    private void removeRequest(TeleportRequest request) {
        Map<UUID, TeleportRequest> requests = requestsByTarget.get(request.targetId());
        if (requests != null) {
            requests.remove(request.requesterId());
            if (requests.isEmpty()) {
                requestsByTarget.remove(request.targetId());
                latestRequesterByTarget.remove(request.targetId());
            } else if (request.requesterId().equals(latestRequesterByTarget.get(request.targetId()))) {
                latestRequesterByTarget.put(request.targetId(), requests.keySet().iterator().next());
            }
        }
        Bukkit.getScheduler().cancelTask(request.expireTaskId());
    }

    private Component buttons(Player viewer, Player requester) {
        String language = messages.languageFor(viewer);
        String acceptText = switch (language) {
            case LanguageProvider.UKRAINIAN -> "[ПРИЙНЯТИ]";
            case LanguageProvider.RUSSIAN -> "[ПРИНЯТЬ]";
            default -> "[ACCEPT]";
        };
        String denyText = switch (language) {
            case LanguageProvider.UKRAINIAN -> "[ВІДХИЛИТИ]";
            case LanguageProvider.RUSSIAN -> "[ОТКЛОНИТЬ]";
            default -> "[DENY]";
        };
        String acceptHover = switch (language) {
            case LanguageProvider.UKRAINIAN -> "Прийняти запит";
            case LanguageProvider.RUSSIAN -> "Принять запрос";
            default -> "Accept request";
        };
        String denyHover = switch (language) {
            case LanguageProvider.UKRAINIAN -> "Відхилити запит";
            case LanguageProvider.RUSSIAN -> "Отклонить запрос";
            default -> "Deny request";
        };

        return Component.text(acceptText, NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/tpyes " + requester.getName()))
                .hoverEvent(HoverEvent.showText(Component.text(acceptHover, NamedTextColor.GREEN)))
                .append(Component.space())
                .append(Component.text(denyText, NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/tpno " + requester.getName()))
                        .hoverEvent(HoverEvent.showText(Component.text(denyHover, NamedTextColor.RED))));
    }

    private Player findPlayer(String name) {
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) {
            return exact;
        }

        List<Player> matches = Bukkit.matchPlayer(name);
        return matches.size() == 1 ? matches.get(0) : null;
    }
}

package com.PlayerTools.playerutils;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class TPA implements Listener {

    private final JavaPlugin plugin;
    // Key = target player, Value = requester
    private final HashMap<UUID, UUID> requests = new HashMap<>();
    private final HashMap<UUID, Integer> expiryTasks = new HashMap<>();

    public TPA(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendRequest(Player requester, Player target) {
        UUID targetId = target.getUniqueId();
        UUID requesterId = requester.getUniqueId();

        // Cancel existing expiry task if there is one
        if (expiryTasks.containsKey(targetId)) {
            Bukkit.getScheduler().cancelTask(expiryTasks.get(targetId));
        }

        requests.put(targetId, requesterId);

        requester.sendMessage(
                Component.text("[TPA] ", NamedTextColor.GOLD)
                        .append(Component.text("Request sent to ", NamedTextColor.WHITE))
                        .append(Component.text(target.getName(), NamedTextColor.AQUA))
        );

        target.sendMessage(
                Component.text("[TPA] ", NamedTextColor.GOLD)
                        .append(Component.text(requester.getName(), NamedTextColor.AQUA))
                        .append(Component.text(" wants to teleport to you! ", NamedTextColor.WHITE))
                        .append(Component.text("/tpaccept", NamedTextColor.GREEN))
                        .append(Component.text(" or ", NamedTextColor.WHITE))
                        .append(Component.text("/tpdeny", NamedTextColor.RED))
        );

        // Expire after 30 seconds
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (requests.containsKey(targetId) && requests.get(targetId).equals(requesterId)) {
                requests.remove(targetId);
                expiryTasks.remove(targetId);
                target.sendMessage(Component.text("[TPA] ", NamedTextColor.GOLD).append(Component.text("Request from ", NamedTextColor.WHITE)).append(Component.text(requester.getName(), NamedTextColor.AQUA)).append(Component.text(" expired.", NamedTextColor.WHITE)));
                if (requester.isOnline()) {
                    requester.sendMessage(Component.text("[TPA] ", NamedTextColor.GOLD).append(Component.text("Your request to ", NamedTextColor.WHITE)).append(Component.text(target.getName(), NamedTextColor.AQUA)).append(Component.text(" expired.", NamedTextColor.WHITE)));
                }
            }
        }, 600L).getTaskId(); // 600 ticks = 30 seconds

        expiryTasks.put(targetId, taskId);
    }

    public void accept(Player target, BackManager backManager) {
        UUID targetId = target.getUniqueId();

        if (!requests.containsKey(targetId)) {
            target.sendMessage(Component.text("[TPA] ", NamedTextColor.GOLD).append(Component.text("No pending request!", NamedTextColor.RED)));
            return;
        }

        Player requester = Bukkit.getPlayer(requests.get(targetId));
        requests.remove(targetId);

        if (expiryTasks.containsKey(targetId)) {
            Bukkit.getScheduler().cancelTask(expiryTasks.get(targetId));
            expiryTasks.remove(targetId);
        }

        if (requester == null || !requester.isOnline()) {
            target.sendMessage(Component.text("[TPA] ", NamedTextColor.GOLD).append(Component.text("That player is no longer online!", NamedTextColor.RED)));
            return;
        }

        // Save back location
        backManager.saveLocation(requester);

        requester.teleport(target.getLocation());

        requester.sendMessage(Component.text("[TPA] ", NamedTextColor.GOLD).append(Component.text("Teleported to ", NamedTextColor.WHITE)).append(Component.text(target.getName(), NamedTextColor.AQUA)));
        target.sendMessage(Component.text("[TPA] ", NamedTextColor.GOLD).append(Component.text(requester.getName(), NamedTextColor.AQUA)).append(Component.text(" has been teleported to you!", NamedTextColor.WHITE)));
    }

    public void deny(Player target) {
        UUID targetId = target.getUniqueId();

        if (!requests.containsKey(targetId)) {
            target.sendMessage(Component.text("[TPA] ", NamedTextColor.GOLD).append(Component.text("No pending request!", NamedTextColor.RED)));
            return;
        }

        Player requester = Bukkit.getPlayer(requests.get(targetId));
        requests.remove(targetId);

        if (expiryTasks.containsKey(targetId)) {
            Bukkit.getScheduler().cancelTask(expiryTasks.get(targetId));
            expiryTasks.remove(targetId);
        }

        target.sendMessage(Component.text("[TPA] ", NamedTextColor.GOLD).append(Component.text("Request denied.", NamedTextColor.RED)));
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(Component.text("[TPA] ", NamedTextColor.GOLD).append(Component.text(target.getName(), NamedTextColor.AQUA)).append(Component.text(" denied your request.", NamedTextColor.RED)));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        requests.remove(uuid);
        if (expiryTasks.containsKey(uuid)) {
            Bukkit.getScheduler().cancelTask(expiryTasks.get(uuid));
            expiryTasks.remove(uuid);
        }
    }
}

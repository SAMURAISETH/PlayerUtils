package com.PlayerTools.playerutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class IgnoreManager implements Listener {

    // Key = player, Value = set of players they are ignoring
    private final HashMap<UUID, HashSet<UUID>> ignoreList = new HashMap<>();

    public void toggle(Player player, Player target) {
        UUID uuid = player.getUniqueId();
        UUID targetId = target.getUniqueId();

        ignoreList.putIfAbsent(uuid, new HashSet<>());

        if (ignoreList.get(uuid).contains(targetId)) {
            ignoreList.get(uuid).remove(targetId);
            player.sendMessage(Component.text("[Ignore] ", NamedTextColor.GOLD)
                    .append(Component.text("You are no longer ignoring ", NamedTextColor.WHITE))
                    .append(Component.text(target.getName(), NamedTextColor.AQUA)));
        } else {
            ignoreList.get(uuid).add(targetId);
            player.sendMessage(Component.text("[Ignore] ", NamedTextColor.GOLD)
                    .append(Component.text("You are now ignoring ", NamedTextColor.WHITE))
                    .append(Component.text(target.getName(), NamedTextColor.AQUA)));
        }
    }

    public boolean isIgnoring(Player player, Player target) {
        UUID uuid = player.getUniqueId();
        UUID targetId = target.getUniqueId();
        return ignoreList.containsKey(uuid) && ignoreList.get(uuid).contains(targetId);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        // Remove ignored players from recipients
        event.getRecipients().removeIf(recipient ->
                recipient instanceof Player r && isIgnoring(r, sender)
        );
    }
}
package com.PlayerTools.playerutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.UUID;

public class BackManager implements Listener {

    private final HashMap<UUID, Location> lastLocations = new HashMap<>();

    public void saveLocation(Player player) {
        lastLocations.put(player.getUniqueId(), player.getLocation());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        lastLocations.put(player.getUniqueId(), player.getLocation());
    }

    public void back(Player player) {
        UUID uuid = player.getUniqueId();

        if (!lastLocations.containsKey(uuid)) {
            player.sendMessage(Component.text("[Back] ", NamedTextColor.GOLD).append(Component.text("No location saved!", NamedTextColor.RED)));
            return;
        }

        Location loc = lastLocations.get(uuid);
        saveLocation(player); // Save current location before teleporting
        player.teleport(loc);
        player.sendMessage(Component.text("[Back] ", NamedTextColor.GOLD).append(Component.text("Teleported to your last location!", NamedTextColor.GREEN)));
    }
}
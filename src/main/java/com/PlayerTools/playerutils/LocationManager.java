package com.PlayerTools.playerutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LocationManager {

    String Plugin_Message = "[Teleport] ";

    private final JavaPlugin plugin;
    private final File dataFile;
    private FileConfiguration data;

    private final HashMap<UUID, HashMap<String, Location>> locations = new HashMap<>();
    private final HashMap<UUID, Map.Entry<String, Location>> pendingConfirm = new HashMap<>();

    public LocationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "locations.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
        loadAll();
    }

    private void loadAll() {
        if (data.getConfigurationSection("locations") == null) return;
        for (String uuidStr : data.getConfigurationSection("locations").getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            locations.put(uuid, new HashMap<>());
            if (data.getConfigurationSection("locations." + uuidStr) == null) continue;
            for (String name : data.getConfigurationSection("locations." + uuidStr).getKeys(false)) {
                Location loc = (Location) data.get("locations." + uuidStr + "." + name);
                if (loc != null) locations.get(uuid).put(name, loc);
            }
        }
    }

    private void save() {
        try { data.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public void saveLocation(Player player, String name, Location loc) {
        UUID uuid = player.getUniqueId();

        if (locations.containsKey(uuid) && locations.get(uuid).containsKey(name)) {
            pendingConfirm.put(uuid, Map.entry(name, loc));
            player.sendMessage(Component.text("[Loc] ", NamedTextColor.GOLD)
                    .append(Component.text("Location ", NamedTextColor.WHITE))
                    .append(Component.text(name, NamedTextColor.AQUA))
                    .append(Component.text(" already exists! Type ", NamedTextColor.WHITE))
                    .append(Component.text("/loc confirm", NamedTextColor.GREEN))
                    .append(Component.text(" to override or ", NamedTextColor.WHITE))
                    .append(Component.text("/loc deny", NamedTextColor.RED))
                    .append(Component.text(" to cancel.", NamedTextColor.WHITE)));
            return;
        }

        for (Map.Entry<UUID, HashMap<String, Location>> entry : locations.entrySet()) {
            if (entry.getKey().equals(uuid)) continue;
            for (Location other : entry.getValue().values()) {
                if (other.getWorld() != null && other.getWorld().equals(loc.getWorld())) {
                    if (other.distance(loc) <= 15) {
                        player.sendMessage(Component.text("[Loc] ", NamedTextColor.GOLD)
                                .append(Component.text("Too close to another player's saved location! Must be at least 15 blocks away.", NamedTextColor.RED)));
                        return;
                    }
                }
            }
        }

        locations.putIfAbsent(uuid, new HashMap<>());
        locations.get(uuid).put(name, loc);
        data.set("locations." + uuid + "." + name, loc);
        save();

        player.sendMessage(Component.text("[Loc] ", NamedTextColor.GOLD)
                .append(Component.text("Location ", NamedTextColor.WHITE))
                .append(Component.text(name, NamedTextColor.AQUA))
                .append(Component.text(" saved!", NamedTextColor.WHITE)));
    }

    public void confirm(Player player) {
        UUID uuid = player.getUniqueId();
        if (!pendingConfirm.containsKey(uuid)) {
            player.sendMessage(Component.text("[Loc] ", NamedTextColor.GOLD)
                    .append(Component.text("Nothing to confirm!", NamedTextColor.RED)));
            return;
        }
        Map.Entry<String, Location> pending = pendingConfirm.remove(uuid);
        locations.putIfAbsent(uuid, new HashMap<>());
        locations.get(uuid).put(pending.getKey(), pending.getValue());
        data.set("locations." + uuid + "." + pending.getKey(), pending.getValue());
        save();
        player.sendMessage(Component.text("[Loc] ", NamedTextColor.GOLD)
                .append(Component.text("Location ", NamedTextColor.WHITE))
                .append(Component.text(pending.getKey(), NamedTextColor.AQUA))
                .append(Component.text(" overridden!", NamedTextColor.GREEN)));
    }

    public void deny(Player player) {
        UUID uuid = player.getUniqueId();
        if (!pendingConfirm.containsKey(uuid)) {
            player.sendMessage(Component.text("[Loc] ", NamedTextColor.GOLD)
                    .append(Component.text("Nothing to deny!", NamedTextColor.RED)));
            return;
        }
        pendingConfirm.remove(uuid);
        player.sendMessage(Component.text("[Loc] ", NamedTextColor.GOLD)
                .append(Component.text("Override cancelled!", NamedTextColor.RED)));
    }

    public void teleport(Player player, String name) {
        UUID uuid = player.getUniqueId();
        if (!locations.containsKey(uuid) || !locations.get(uuid).containsKey(name)) {
            player.sendMessage(Component.text(Plugin_Message, NamedTextColor.GOLD)
                    .append(Component.text("Location not found!", NamedTextColor.RED)));
            return;
        }
        player.teleport(locations.get(uuid).get(name));
        player.sendMessage(Component.text(Plugin_Message, NamedTextColor.GOLD)
                .append(Component.text("Teleported to ", NamedTextColor.WHITE))
                .append(Component.text(name, NamedTextColor.AQUA)));
    }

    public void delete(Player player, String name) {
        UUID uuid = player.getUniqueId();
        if (!locations.containsKey(uuid) || !locations.get(uuid).containsKey(name)) {
            player.sendMessage(Component.text(Plugin_Message, NamedTextColor.GOLD)
                    .append(Component.text("Location not found!", NamedTextColor.RED)));
            return;
        }
        locations.get(uuid).remove(name);
        data.set("locations." + uuid + "." + name, null);
        save();
        player.sendMessage(Component.text(Plugin_Message, NamedTextColor.GOLD)
                .append(Component.text("Location ", NamedTextColor.WHITE))
                .append(Component.text(name, NamedTextColor.AQUA))
                .append(Component.text(" deleted!", NamedTextColor.WHITE)));
    }

    public void list(Player player) {
        UUID uuid = player.getUniqueId();
        if (!locations.containsKey(uuid) || locations.get(uuid).isEmpty()) {
            player.sendMessage(Component.text(Plugin_Message, NamedTextColor.GOLD)
                    .append(Component.text("You have no saved locations!", NamedTextColor.RED)));
            return;
        }
        player.sendMessage(Component.text("-- Your Locations --", NamedTextColor.GOLD));
        for (Map.Entry<String, Location> entry : locations.get(uuid).entrySet()) {
            Location loc = entry.getValue();
            player.sendMessage(Component.text("- ", NamedTextColor.YELLOW)
                    .append(Component.text(entry.getKey(), NamedTextColor.AQUA))
                    .append(Component.text(" | ", NamedTextColor.GRAY))
                    .append(Component.text(loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(), NamedTextColor.WHITE)));
        }
    }
}
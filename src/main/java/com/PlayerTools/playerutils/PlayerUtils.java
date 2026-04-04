package com.PlayerTools.playerutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerUtils extends JavaPlugin implements CommandExecutor {

    private TPA tpa;
    private BackManager backManager;
    private LocationManager locationManager;
    private IgnoreManager ignoreManager;
    String Plugin_Message = "[PlayerUtils] ";

    @Override
    public void onEnable() {
        getLogger().info("PlayerUtils has been enabled!");
        backManager = new BackManager();
        tpa = new TPA(this);
        locationManager = new LocationManager(this);
        ignoreManager = new IgnoreManager();

        getServer().getPluginManager().registerEvents(tpa, this);
        getServer().getPluginManager().registerEvents(backManager, this);
        getServer().getPluginManager().registerEvents(ignoreManager, this);

        getCommand("loc").setExecutor(this);
        getCommand("tpa").setExecutor(this);
        getCommand("tpaccept").setExecutor(this);
        getCommand("tpdeny").setExecutor(this);
        getCommand("back").setExecutor(this);
        getCommand("ignore").setExecutor(this);
    }

    @Override
    public void onDisable() {
        getLogger().info("PlayerUtils has been disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "tpa" -> {
                if (args.length < 1) {
                    player.sendMessage(Component.text("[TPA] ", NamedTextColor.GOLD).append(Component.text("Usage: /tpa <player>", NamedTextColor.RED)));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(Component.text("[TPA] ", NamedTextColor.GOLD).append(Component.text("Player not found!", NamedTextColor.RED)));
                    return true;
                }
                if (target.equals(player)) {
                    player.sendMessage(Component.text("[TPA] ", NamedTextColor.GOLD).append(Component.text("You can't TPA to yourself!", NamedTextColor.RED)));
                    return true;
                }
                tpa.sendRequest(player, target);
            }
            case "tpaccept" -> tpa.accept(player, backManager);
            case "tpdeny" -> tpa.deny(player);
            case "back" -> backManager.back(player);
            case "ignore" -> {
                if (args.length < 1) {
                    player.sendMessage(Component.text("[Ignore] ", NamedTextColor.GOLD).append(Component.text("Usage: /ignore <player>", NamedTextColor.RED)));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(Component.text("[Ignore] ", NamedTextColor.GOLD).append(Component.text("Player not found!", NamedTextColor.RED)));
                    return true;
                }
                if (target.equals(player)) {
                    player.sendMessage(Component.text("[Ignore] ", NamedTextColor.GOLD).append(Component.text("You can't ignore yourself!", NamedTextColor.RED)));
                    return true;
                }
                ignoreManager.toggle(player, target);
            }
            case "loc" -> {
                if (args.length < 1) {
                    player.sendMessage(Component.text("[Loc] ", NamedTextColor.GOLD)
                            .append(Component.text("Usage: /loc <save|tp|delete|list|confirm|deny> [name] [x y z]", NamedTextColor.RED)));
                    return true;
                }
                switch (args[0].toLowerCase()) {
                    case "save" -> {
                        if (args.length < 2) {
                            player.sendMessage(Component.text("[Loc] ", NamedTextColor.GOLD)
                                    .append(Component.text("Usage: /loc save <name> [x y z]", NamedTextColor.RED)));
                            return true;
                        }
                        if (args.length == 5) {
                            try {
                                double x = Double.parseDouble(args[2]);
                                double y = Double.parseDouble(args[3]);
                                double z = Double.parseDouble(args[4]);
                                Location loc = new Location(player.getWorld(), x, y, z);
                                locationManager.saveLocation(player, args[1], loc);
                            } catch (NumberFormatException e) {
                                player.sendMessage(Component.text("[Loc] ", NamedTextColor.GOLD)
                                        .append(Component.text("Invalid coordinates!", NamedTextColor.RED)));
                            }
                        } else {
                            locationManager.saveLocation(player, args[1], player.getLocation());
                        }
                    }
                    case "tp" -> {
                        if (args.length < 2) {
                            player.sendMessage(Component.text("[Loc] ", NamedTextColor.GOLD)
                                    .append(Component.text("Usage: /loc tp <name>", NamedTextColor.RED)));
                            return true;
                        }
                        locationManager.teleport(player, args[1]);
                    }
                    case "delete" -> {
                        if (args.length < 2) {
                            player.sendMessage(Component.text("[Loc] ", NamedTextColor.GOLD)
                                    .append(Component.text("Usage: /loc delete <name>", NamedTextColor.RED)));
                            return true;
                        }
                        locationManager.delete(player, args[1]);
                    }
                    case "list" -> locationManager.list(player);
                    case "confirm" -> locationManager.confirm(player);
                    case "deny" -> locationManager.deny(player);
                    default -> player.sendMessage(Component.text("[Loc] ", NamedTextColor.GOLD)
                            .append(Component.text("Usage: /loc <save|tp|delete|list|confirm|deny> [name] [x y z]", NamedTextColor.RED)));
                }
            }
        }
        return true;
    }
}
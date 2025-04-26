package com.hamta.spectatorborderguard;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class SpectatorBorderGuard extends JavaPlugin {

    private BukkitTask task;

    private int checkIntervalTicks;
    private double borderMargin;
    private String actionbarMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        checkIntervalTicks = getConfig().getInt("check_interval_ticks", 40);
        borderMargin = getConfig().getDouble("border_margin", 1.0);
        actionbarMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("actionbar_message", "&cYou cannot leave the world border in spectator mode."));

        getLogger().info("SpectatorBorderGuard enabled.");

        task = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            List<? extends Player> players = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.getGameMode() == GameMode.SPECTATOR)
                    .toList();

            if (players.isEmpty()) return;

            for (Player player : players) {
                World world = player.getWorld();
                WorldBorder border = world.getWorldBorder();
                double size = border.getSize() / 2.0;
                Location center = border.getCenter();
                Location loc = player.getLocation();

                double dx = Math.abs(loc.getX() - center.getX());
                double dz = Math.abs(loc.getZ() - center.getZ());

                if (dx > size - borderMargin || dz > size - borderMargin) {
                    double safeX = Math.max(Math.min(loc.getX(), center.getX() + size - borderMargin), center.getX() - size + borderMargin);
                    double safeZ = Math.max(Math.min(loc.getZ(), center.getZ() + size - borderMargin), center.getZ() - size + borderMargin);
                    Location safeLoc = new Location(world, safeX, loc.getY(), safeZ, loc.getYaw(), loc.getPitch());

                    Bukkit.getScheduler().runTask(this, () -> {
                        player.teleportAsync(safeLoc).thenAccept(success -> {
                            if (success) {
                                player.sendActionBar(actionbarMessage);
                            }
                        });
                    });
                }
            }

        }, 0L, checkIntervalTicks);
    }

    @Override
    public void onDisable() {
        if (task != null) task.cancel();
        getLogger().info("SpectatorBorderGuard disabled.");
    }
}

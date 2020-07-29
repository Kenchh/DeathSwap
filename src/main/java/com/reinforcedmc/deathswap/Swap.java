package com.reinforcedmc.deathswap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

public class Swap extends BukkitRunnable {

    private final long interval;
    private long remaining;

    public Swap(long interval) {
        this.interval = interval;
        this.remaining = interval;
    }

    public void start() {
        this.runTaskTimer(DeathSwap.getInstance(), 0, 20);
    }

    @Override
    public void run() {
        if (remaining <= 0) {
            Bukkit.broadcastMessage(ChatColor.RED + ChatColor.BOLD.toString() + "Players have been swapped!");
            DeathSwap.swap();
            Bukkit.getOnlinePlayers().forEach((p) -> p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.8F));

            remaining = interval;
        } else {

            if (remaining == 30) {
                String text = String.format(ChatColor.RED + ChatColor.BOLD.toString() + "Death Swap will ocurr in %s seconds.", remaining);
                Bukkit.broadcastMessage(text);
                Bukkit.getOnlinePlayers().forEach((p) -> p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1F, 1F));
            }

            if (remaining <= 5) {
                String text = String.format(ChatColor.RED + ChatColor.BOLD.toString() + "Death Swap will ocurr in %s seconds.", remaining);
                Bukkit.broadcastMessage(text);
                Bukkit.getOnlinePlayers().forEach((p) -> p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1F, 1F));
            }

            remaining--;
        }
    }

}

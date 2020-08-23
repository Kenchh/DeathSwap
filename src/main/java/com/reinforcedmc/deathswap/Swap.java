package com.reinforcedmc.deathswap;

import com.reinforcedmc.gameapi.GameAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.UUID;

public class Swap extends BukkitRunnable {

    public final long interval;
    public long remaining;

    public Swap(long interval) {
        this.interval = interval;
        this.remaining = interval;
    }

    public void start() {
        this.runTaskTimer(DeathSwap.getInstance(), 0, 20);
    }

    int itemdropinterval = 90;
    int range = 10;

    @Override
    public void run() {

        if(remaining % itemdropinterval == 0) {
            for(UUID uuid : DeathSwap.ingame) {
                Player p = Bukkit.getPlayer(uuid);

                Vector offset = new Vector();

                int xOff = new Random().nextInt(range) + 1;
                if (xOff <= range/2) xOff = -xOff;

                offset.setX(xOff);
                offset.setY(0);

                int zOff = new Random().nextInt(range) + 1;
                if (zOff <= range/2) zOff = -zOff;

                offset.setZ(zOff);

                GameItem randomItem = DeathSwap.getInstance().getGameItemManager().gameItems.get(new Random().nextInt(DeathSwap.getInstance().getGameItemManager().gameItems.size()));
                randomItem.drop(new Location(p.getLocation().getWorld(), p.getLocation().getX() + xOff, 200, p.getLocation().getZ() + zOff));
            }
        }

        if (remaining <= 0) {
            Bukkit.broadcastMessage(ChatColor.RED + ChatColor.BOLD.toString() + "Players have been swapped!");
            DeathSwap.swap();
            Bukkit.getOnlinePlayers().forEach((p) -> p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.8F));

            remaining = interval;
        } else {

            if (remaining == 60) {
                String text = String.format(ChatColor.RED + ChatColor.BOLD.toString() + "Death Swap will ocurr in %s seconds.", remaining);
                Bukkit.broadcastMessage(text);
                Bukkit.getOnlinePlayers().forEach((p) -> p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1F, 1F));
            }

            if (remaining == 30) {
                String text = String.format(ChatColor.RED + ChatColor.BOLD.toString() + "Death Swap will ocurr in %s seconds.", remaining);
                Bukkit.broadcastMessage(text);
                Bukkit.getOnlinePlayers().forEach((p) -> p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1F, 1F));
            }

            if (remaining == 15) {
                String text = String.format(ChatColor.RED + ChatColor.BOLD.toString() + "Death Swap will ocurr in %s seconds.", remaining);
                Bukkit.broadcastMessage(text);
                Bukkit.getOnlinePlayers().forEach((p) -> p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1F, 1F));
            }

            if (remaining <= 5) {
                String text = String.format(ChatColor.RED + ChatColor.BOLD.toString() + "Death Swap will ocurr in %s seconds.", remaining);
                Bukkit.broadcastMessage(text);
                for(int i=0;i<=3;i++) {
                    Bukkit.getOnlinePlayers().forEach((p) -> p.playSound(p.getLocation(), Sound.BLOCK_STONE_PLACE, 1F, 0.6F));
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Bukkit.getOnlinePlayers().forEach((p) -> p.playSound(p.getLocation(), Sound.BLOCK_STONE_PLACE, 1F, 0.1F));
                        }
                    }.runTaskLater(GameAPI.getInstance(), 5L);
                }
            }

            remaining--;
        }
    }

}

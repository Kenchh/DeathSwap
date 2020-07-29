package com.reinforcedmc.deathswap;

import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public final class DeathSwap extends JavaPlugin implements Listener {

    public static ArrayList<UUID> ingame = new ArrayList<>();
    private Swap swap;

    private static DeathSwap instance;

    @Override
    public void onEnable() {
        instance = this;

        log("Enabled!");
        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        start();
    }

    @Override
    public void onDisable() {
        log("Disabled!");
    }

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().size() > 2) {
                    Bukkit.getOnlinePlayers().forEach((p) -> ingame.add(p.getUniqueId()));

                    swap = new Swap(5);
                    swap.start();

                    Bukkit.broadcastMessage(ChatColor.RED + ChatColor.BOLD.toString() + "Death Swap has started. Last one to survive wins!");

                    this.cancel();
                }
            }
        }.runTaskTimer(this, 0, 1);
    }

    public static void log(Object object) {
        System.out.println("[DeathSwap] " + object.toString());
    }

    public static DeathSwap getInstance() {
        return instance;
    }

    public static void swap() {

        HashMap<Player, Player> players = new HashMap<>();
        HashMap<Player, Location> plocs = new HashMap<>();
        ArrayList<UUID> reserved = new ArrayList<>();

        for(UUID uuid : ingame) {
            Player p = Bukkit.getPlayer(uuid);

            int maxtries = 3;
            UUID tp = p.getUniqueId();
            if(ingame.size() >= 3) {
                while ((tp.equals(uuid) || reserved.contains(tp) || (players.containsKey(Bukkit.getPlayer(tp)) && players.get(Bukkit.getPlayer(tp)).getUniqueId() == uuid)) && maxtries > 0) {
                    tp = ingame.get(new Random().nextInt(ingame.size()));
                    maxtries--;
                }
            } else {
                while ((tp.equals(uuid) || reserved.contains(tp)) && maxtries > 3) {
                    tp = ingame.get(new Random().nextInt(ingame.size()));
                    maxtries--;
                }
            }
            players.put(p, Bukkit.getPlayer(tp));
            reserved.add(tp);
            plocs.put(p, Bukkit.getPlayer(tp).getLocation());
        }

        for(Player p : plocs.keySet()) {
            p.teleport(plocs.get(p));
        }

    }

    public static ArrayList<Player> getAlive() {
        ArrayList<Player> alive = new ArrayList<>();
        for (UUID uuid : ingame){
            Player player = Bukkit.getServer().getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            alive.add(player);
        }

        return alive;
    }

    /*
    NETHER CANCEL
     */
    @EventHandler
    public void onPortal(PlayerPortalEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {

        if (e.getCause().equals(PlayerTeleportEvent.TeleportCause.END_PORTAL))
            e.setCancelled(true);
        if (e.getCause().equals(PlayerTeleportEvent.TeleportCause.NETHER_PORTAL))
            e.setCancelled(true);
        if (e.getCause().equals(PlayerTeleportEvent.TeleportCause.END_GATEWAY))
            e.setCancelled(true);

    }

    /*
    PVP CANCEL
     */
    @EventHandler
    public void onPvP(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (!(e.getDamager() instanceof Player)) return;

        e.setCancelled(true);
        e.getDamager().sendMessage(ChatColor.RED + "PvP is disabled in Death Swap!");
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {

        if (!getAlive().isEmpty()) {
            Player toTeleport = getAlive().get(new Random().nextInt(getAlive().size()));
            e.setRespawnLocation(toTeleport.getLocation());
        }

    }

    @EventHandler
    public void onDie(PlayerDeathEvent e) {
        Player p = e.getEntity();

        if(ingame.contains(p.getUniqueId())) {

            ingame.remove(p.getUniqueId());
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> p.spigot().respawn(), 1L);

            Bukkit.broadcastMessage(ChatColor.RED + ChatColor.BOLD.toString() + p.getName() + " has died! " + ingame.size() + " remaining.");
            p.setGameMode(GameMode.SPECTATOR);

            update();
        }
    }

    private void update() {

        if (getAlive().size() == 1) {

            Player winner = getAlive().get(0);
            Bukkit.broadcastMessage(ChatColor.GREEN + ChatColor.BOLD.toString() + winner.getName() + " has won!");
            swap.cancel();

            Bukkit.getOnlinePlayers().forEach((loser) -> loser.teleport(winner));

            new BukkitRunnable() {
                int i = 5;
                @Override
                public void run() {
                    if (i > 0) {
                        winner.getWorld().spawnEntity(winner.getLocation(), EntityType.FIREWORK);
                        i--;
                    } else {
                        this.cancel();
                    }
                }
            }.runTaskTimer(this, 0, 20);
        }

    }

    @EventHandler
    public void onLog(PlayerQuitEvent e) {
        if (!ingame.contains(e.getPlayer().getUniqueId())) return;
        ingame.remove(e.getPlayer().getUniqueId());
        update();
    }
}

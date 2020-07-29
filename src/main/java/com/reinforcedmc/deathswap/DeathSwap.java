package com.reinforcedmc.deathswap;

import com.reinforcedmc.gameapi.GameAPI;
import com.reinforcedmc.gameapi.GameStatus;
import com.reinforcedmc.gameapi.events.GameSetupEvent;
import com.reinforcedmc.gameapi.events.GameStartEvent;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
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

    }

    @Override
    public void onDisable() {
        log("Disabled!");
    }

    @EventHandler
    public void onStart(GameStartEvent e) {
        start();
    }

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().size() > 1) {
                    Bukkit.getOnlinePlayers().forEach((p) -> ingame.add(p.getUniqueId()));

                    swap = new Swap(15);
                    swap.start();

                    Bukkit.broadcastMessage(GameAPI.getInstance().currentGame.getPrefix() + ChatColor.GRAY + " has started. " + ChatColor.YELLOW + "Last one to survive wins!");

                    this.cancel();
                } else {
                    update();
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
                /* Loop, that searches for a valid player to teleport to. */
                while ((tp.equals(uuid) || reserved.contains(tp) || (players.containsKey(Bukkit.getPlayer(tp)) && players.get(Bukkit.getPlayer(tp)).getUniqueId() == uuid)) && maxtries > 0) {
                    tp = ingame.get(new Random().nextInt(ingame.size()));
                    maxtries--;
                }
            } else {
                for(Player pp : Bukkit.getOnlinePlayers()) {
                    if(!p.getUniqueId().equals(pp.getUniqueId())) {
                        tp = pp.getUniqueId();
                        break;
                    }
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

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if(GameAPI.getInstance().status == GameStatus.POSTCOUNTDOWN && e.getFrom().distance(e.getTo()) != 0) {
            e.setCancelled(true);
        }
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
            swap.cancel();
            ingame.clear();
            GameAPI.getInstance().getAPI().endGame(winner);

        }

        if(ingame.isEmpty()) {
            swap.cancel();
            GameAPI.getInstance().getAPI().endGame(null);
        }

    }

    @EventHandler
    public void onLog(PlayerQuitEvent e) {
        if (!ingame.contains(e.getPlayer().getUniqueId())) return;
        ingame.remove(e.getPlayer().getUniqueId());
        update();
    }
}

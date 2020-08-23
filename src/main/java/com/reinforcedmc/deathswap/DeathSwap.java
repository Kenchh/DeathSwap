package com.reinforcedmc.deathswap;

import com.reinforcedmc.gameapi.GameAPI;
import com.reinforcedmc.gameapi.GameStatus;
import com.reinforcedmc.gameapi.events.GamePreStartEvent;
import com.reinforcedmc.gameapi.events.GameSetupEvent;
import com.reinforcedmc.gameapi.events.GameStartEvent;
import com.reinforcedmc.gameapi.scoreboard.UpdateScoreboardEvent;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public final class DeathSwap extends JavaPlugin implements Listener {

    public static ArrayList<UUID> ingame = new ArrayList<>();
    private Swap swap;

    private static DeathSwap instance;

    private World world;
    private Location spawn;
    private long maxRadius;

    private GameItemManager gameItemManager;

    @Override
    public void onEnable() {
        instance = this;

        log("Enabled!");
        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        this.gameItemManager = new GameItemManager();
    }

    @Override
    public void onDisable() {
        log("Disabled!");
    }

    @EventHandler
    public void onSetup(GameSetupEvent e) {
        if(!GameAPI.getInstance().currentGame.getName().equalsIgnoreCase("DeathSwap")) return;

        createWorld();
        e.openServer();
    }

    public void createWorld() {

        if(Bukkit.getWorld("Game") != null) {
            Bukkit.unloadWorld("Game", false);
        }
        File folder = new File(Bukkit.getWorldContainer() + "/Game");
        try {
            FileUtils.deleteDirectory(folder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        WorldCreator creator = new WorldCreator("Game");
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(true);
        world = creator.createWorld();

        spawn = new Location(world, 0, 0, 0);
        maxRadius = 250;

    }

    @EventHandler
    public void onPreStart(GamePreStartEvent e) {
        if(!GameAPI.getInstance().currentGame.getName().equalsIgnoreCase("DeathSwap")) return;

        for (UUID game : GameAPI.getInstance().ingame) {

            Player p = Bukkit.getServer().getPlayer(game);
            if (p == null || !p.isOnline()) continue;

            boolean notocean = false;

            Location location = Bukkit.getWorld("Game").getSpawnLocation();

            while(!notocean) {
                location = new Location(world, 0, 0, 0); // New Location in the right World you want
                location.setX(spawn.getX() + Math.random() * maxRadius * 2 - maxRadius); // This get a Random with a MaxRange
                location.setZ(spawn.getZ() + Math.random() * maxRadius * 2 - maxRadius);

                Block highest = world.getHighestBlockAt(location.getBlockX(), location.getBlockZ());

                if(highest.isLiquid()) {
                    maxRadius += 100;
                    continue;
                }

                notocean = true;
                location.setY(highest.getY() + 1); // Get the Highest Block of the Location for Save Spawn.
            }

            p.teleport(location);

        }

        new com.reinforcedmc.gameapi.GamePostCountDown().start();

    }

    @EventHandler
    public void onStart(GameStartEvent e) {
        if(!GameAPI.getInstance().currentGame.getName().equalsIgnoreCase("DeathSwap")) return;

        start();
    }


    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (GameAPI.getInstance().ingame.size() > 1) {

                    ingame.clear();

                    for(UUID uuid : GameAPI.getInstance().ingame) {
                        ingame.add(uuid);
                    }

                    swap = new Swap(15);
                    swap.start();

                    Bukkit.broadcastMessage(GameAPI.getInstance().currentGame.getPrefix() + ChatColor.GRAY + " has started. " + ChatColor.YELLOW + "Last one to survive wins!");

                    this.cancel();
                } else {
                    GameAPI.getInstance().getAPI().endGame(null);
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

    static HashMap<Player, Player> players = new HashMap<>();

    public static void swap() {

        players.clear();

        HashMap<Player, Location> plocs = new HashMap<>();
        ArrayList<UUID> reserved = new ArrayList<>();

        for(UUID uuid : ingame) {
            Player p = Bukkit.getPlayer(uuid);

            UUID tp = p.getUniqueId();
            if(ingame.size() >= 3) {
                /* Loop, that searches for a valid player to teleport to. */
                while ((tp.equals(uuid) || reserved.contains(tp) || (players.containsKey(Bukkit.getPlayer(tp)) && players.get(Bukkit.getPlayer(tp)).getUniqueId() == uuid))) {
                    tp = ingame.get(new Random().nextInt(ingame.size()));
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

    /*
    NETHER CANCEL
     */
    @EventHandler
    public void onPortal(PlayerPortalEvent e) {
        if(!GameAPI.getInstance().currentGame.getName().equalsIgnoreCase("DeathSwap")) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if(!GameAPI.getInstance().currentGame.getName().equalsIgnoreCase("DeathSwap")) return;

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
        if(!GameAPI.getInstance().currentGame.getName().equalsIgnoreCase("DeathSwap")) return;

        if (!(e.getEntity() instanceof Player)) return;
        if (!(e.getDamager() instanceof Player)) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if(!GameAPI.getInstance().currentGame.getName().equalsIgnoreCase("DeathSwap")) return;

        if(GameAPI.getInstance().status == GameStatus.POSTCOUNTDOWN || GameAPI.getInstance().status == GameStatus.ENDING) {
            if(GameAPI.getInstance().ingame.contains(e.getPlayer().getUniqueId())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if(!GameAPI.getInstance().currentGame.getName().equalsIgnoreCase("DeathSwap")) return;

        if(GameAPI.getInstance().status == GameStatus.POSTCOUNTDOWN || GameAPI.getInstance().status == GameStatus.ENDING) {
            if(GameAPI.getInstance().ingame.contains(e.getPlayer().getUniqueId())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if(!GameAPI.getInstance().currentGame.getName().equalsIgnoreCase("DeathSwap")) return;

        if (!getAlive().isEmpty()) {
            Player toTeleport = getAlive().get(new Random().nextInt(getAlive().size()));
            e.setRespawnLocation(toTeleport.getLocation());
        }

    }

    @EventHandler
    public void onDie(PlayerDeathEvent e) {
        if(!GameAPI.getInstance().currentGame.getName().equalsIgnoreCase("DeathSwap")) return;

        Player p = e.getEntity();

        if(ingame.contains(p.getUniqueId())) {

            ingame.remove(p.getUniqueId());
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> p.spigot().respawn(), 1L);

            e.setDeathMessage(null);

            if(getAlive().size() > 1) {
                if (swap.interval - swap.remaining > 60) {
                    e.setDeathMessage(ChatColor.RED + ChatColor.BOLD.toString() + p.getName() + " has died! " + ingame.size() + " remaining.");
                } else {
                    e.setDeathMessage(ChatColor.RED + ChatColor.BOLD.toString() + p.getName() + " died to " + players.get(p).getName() + "'s trap.");
                }
            }

            GameAPI.getInstance().getAPI().putInSpectator(p);

            update();
        }
    }

    private void update() {

        if(GameAPI.getInstance().status != GameStatus.INGAME) return;

        if (getAlive().size() == 1) {

            Player winner = getAlive().get(0);
            swap.cancel();
            GameAPI.getInstance().getAPI().endGame(winner);

        }

        if(ingame.isEmpty()) {

            swap.cancel();
            GameAPI.getInstance().getAPI().endGame(null);

        }

    }

    public GameItemManager getGameItemManager() {
        return gameItemManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if(!GameAPI.getInstance().currentGame.getName().equalsIgnoreCase("DeathSwap")) return;

        Player p = e.getPlayer();

        if (!getAlive().isEmpty()) {

            GameAPI.getInstance().getAPI().putInSpectator(p);

            Player toTeleport = getAlive().get(new Random().nextInt(getAlive().size()));
            p.teleport(toTeleport);
        }

    }

    @EventHandler
    public void onLog(PlayerQuitEvent e) {
        if(!GameAPI.getInstance().currentGame.getName().equalsIgnoreCase("DeathSwap")) return;

        if (!ingame.contains(e.getPlayer().getUniqueId())) return;
        ingame.remove(e.getPlayer().getUniqueId());
        update();
    }

    @EventHandler
    public void onSBUpdate(UpdateScoreboardEvent e) {

        if (GameAPI.getInstance().currentGame.getName() != "DeathSwap" || GameAPI.getInstance().status != GameStatus.INGAME) {
            return;
        }

        String[] scoreboard = {
                "",
                String.format("&bPlayers remaining: &f%s", ingame.size()),
                ""
        };

        e.getScoreboard().setLines(scoreboard);
    }
}

package com.reinforcedmc.deathswap;

import com.reinforcedmc.gameapi.GameAPI;
import com.reinforcedmc.gameapi.api.GameWorld;
import com.reinforcedmc.gameapi.events.api.GamePreStartEvent;
import com.reinforcedmc.gameapi.events.api.GameSetupEvent;
import com.reinforcedmc.gameapi.events.api.GameStartEvent;
import com.reinforcedmc.gameapi.game.GamePostCountDown;
import com.reinforcedmc.gameapi.game.GameStatus;
import com.reinforcedmc.gameapi.scoreboard.UpdateScoreboardEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public final class DeathSwap extends JavaPlugin implements Listener {

    public static ArrayList<UUID> ingame = new ArrayList<>();
    private Swap swap;

    private static DeathSwap instance;
    private GameWorld gameWorld;
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

        gameWorld = new GameWorld("Game", 250, false, false);
        e.openServer();
    }

    @EventHandler
    public void onPreStart(GamePreStartEvent e) {
        if(!GameAPI.getInstance().currentGame.getName().equalsIgnoreCase("DeathSwap")) return;

        gameWorld.teleportPlayers();
        new GamePostCountDown().start();

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

                    swap = new Swap(5);
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

    static HashMap<Player, PlayerLoc> players = new HashMap<>();

    public static void swap() {

        Collections.shuffle(ingame);;

        int i = 0;
        for(UUID id : ingame) {
            Player p = Bukkit.getPlayer(id);

            if(i < ingame.size() - 1) {
                players.put(p, new PlayerLoc(Bukkit.getPlayer(ingame.get(i+1)), Bukkit.getPlayer(ingame.get(i+1)).getLocation()));
            } else {
                players.put(Bukkit.getPlayer(ingame.get(i)), new PlayerLoc(Bukkit.getPlayer(ingame.get(0)), Bukkit.getPlayer(ingame.get(0)).getLocation()));
            }

            i++;
        }

        for(Player p : players.keySet()) {
            Location tp = players.get(p).loc;
            p.teleport(tp);
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
                    e.setDeathMessage(ChatColor.RED + ChatColor.BOLD.toString() + p.getName() + " died to " + players.get(p).p.getName() + "'s trap.");
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

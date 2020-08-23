package com.reinforcedmc.deathswap.gameitems;

import com.reinforcedmc.deathswap.GameItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public class CreeperEgg extends GameItem {

    public CreeperEgg() {
        super(ChatColor.GREEN + ChatColor.BOLD.toString() + "Creeper", Material.CREEPER_SPAWN_EGG, null);
    }

}

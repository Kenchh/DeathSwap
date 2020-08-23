package com.reinforcedmc.deathswap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class GameItem extends ItemStack {

    public GameItem(String displayname, Material material, List<String> lore) {
        this.setType(material);
        this.setAmount(1);

        ItemMeta meta = this.getItemMeta();
        meta.setDisplayName(displayname);

        if(lore != null)
            meta.setLore(lore);

        this.setItemMeta(meta);
    }

    public void drop(Location loc) {
        Item i = Bukkit.getWorld(loc.getWorld().getName()).dropItem(loc, this);
        i.setGlowing(true);
    }

}

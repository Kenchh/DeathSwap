package com.reinforcedmc.deathswap;

import com.reinforcedmc.deathswap.gameitems.CreeperEgg;

import java.util.ArrayList;

public class GameItemManager {

    public ArrayList<GameItem> gameItems = new ArrayList<>();

    public GameItemManager() {
        gameItems.add(new CreeperEgg());
    }

}

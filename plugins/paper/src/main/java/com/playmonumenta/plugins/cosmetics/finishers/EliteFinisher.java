package com.playmonumenta.plugins.cosmetics.finishers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface EliteFinisher {

	void run(Player p, Entity killedMob, Location loc);

	Material getDisplayItem();

}

package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class USAFireworkFinisher implements EliteFinisher {

	public static final String NAME = "Patriot Firework";

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		EntityUtils.fireworkAnimation(loc, List.of(Color.RED, Color.BLUE, Color.WHITE), FireworkEffect.Type.BURST, 40);
	}

	@Override
	public Material getDisplayItem() {
		return Material.FIREWORK_ROCKET;
	}

}

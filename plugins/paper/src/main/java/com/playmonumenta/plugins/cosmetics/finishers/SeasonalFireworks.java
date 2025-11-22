package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class SeasonalFireworks implements EliteFinisher {
	public static final String NAME = "Seasonal Fireworks";

	public static final List<List<Color>> SEASON_COLORS = List.of(
		// Spring
		colorsFromInts(List.of(0x5ea919, 0x687635)),
		// Summer
		colorsFromInts(List.of(0xf1af15, 0xba8523)),
		// Fall
		colorsFromInts(List.of(0xa25426, 0x8f3d2f)),
		// Winter
		colorsFromInts(List.of(0x8fb4fb, 0xb6cdfe))
	);

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		EntityUtils.fireworkAnimation(loc, FastUtils.getRandomElement(SEASON_COLORS), FireworkEffect.Type.BURST, 40);
	}

	@Override
	public Material getDisplayItem() {
		return Material.FIREWORK_ROCKET;
	}

	public static List<Color> colorsFromInts(List<Integer> colorInts) {
		return colorInts.stream().map(Color::fromRGB).toList();
	}
}

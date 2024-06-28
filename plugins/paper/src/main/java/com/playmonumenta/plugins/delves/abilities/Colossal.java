package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.particle.PPRectPrism;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;

public class Colossal {

	private static final double SPAWN_CHANCE_PER_LEVEL = 0.05;
	private static final LoSPool COLOSSAL_LAND_POOL = new LoSPool.LibraryPool("~DelveColossalLand");
	private static final LoSPool COLOSSAL_WATER_POOL = new LoSPool.LibraryPool("~DelveColossalWater");

	public static final String DESCRIPTION = "Broken spawners unleash enemies.";

	public static Component[] rankDescription(int level) {
		return new Component[]{
			Component.text("Broken Spawners have a " + Math.round(SPAWN_CHANCE_PER_LEVEL * level * 100) + "% chance"),
			Component.text("to spawn Colossi.")
		};
	}

	public static void applyModifiers(Location blockLoc, int level) {
		if (level == 0 || FastUtils.RANDOM.nextDouble() > SPAWN_CHANCE_PER_LEVEL * level) {
			return;
		}

		Location loc = blockLoc.toCenterLocation();

		int airBlocks = 0;
		int waterBlocks = 0;

		for (int x = -2; x <= 2; x++) {
			for (int y = -2; y <= 2; y++) {
				for (int z = -2; z <= 2; z++) {
					Location locClone = loc.clone().add(x, y, z);
					if (locClone.getBlock().getType() == Material.WATER) {
						waterBlocks++;
					}

					if (locClone.getBlock().getType() == Material.AIR) {
						airBlocks++;
					}
				}
			}
		}

		new PartialParticle(Particle.FLASH, loc).minimumCount(1).spawnAsEnemy();
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 30, 0.2, 0.2, 0.2, 0.1).spawnAsEnemy();

		new PPRectPrism(Particle.REDSTONE, loc.clone().add(-0.5, -0.5, -0.5), loc.clone().add(0.5, 0.5, 0.5))
				.countPerMeter(20).edgeMode(true).gradientColor(Color.fromRGB(247, 188, 37), Color.fromRGB(235, 69, 28), 0.75f)
				.data(new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1f)).spawnAsEnemy();

		final int mAir = airBlocks;
		final int mWater = waterBlocks;
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mAir >= mWater) {
					COLOSSAL_LAND_POOL.spawn(loc);
				} else {
					COLOSSAL_WATER_POOL.spawn(loc);
				}
			}
		}.runTaskLater(Plugin.getInstance(), 20);
	}

}

package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class Legionary {

	private static final double SPAWN_CHANCE_PER_LEVEL = 0.15;

	public static final String DESCRIPTION = "Enemies come in larger numbers.";

	public static String[] rankDescription(int level) {
			return new String[]{
				"Spawners have a " + Math.round(SPAWN_CHANCE_PER_LEVEL * level * 100) + "% chance",
				"to spawn a copy of each enemy."
			};
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		if (DelvesUtils.isDelveMob(mob) || EntityUtils.isBoss(mob)) {
			return;
		}

		for (int i = FastUtils.roundRandomly(SPAWN_CHANCE_PER_LEVEL * level); i > 0; i--) {
			Location spawningLoc = mob.getLocation().clone();

			// don't spawn directly in the mob, and try 20 times to find an open spot
			for (int j = 0; j < 20; j++) {
				double r = FastUtils.randomDoubleInRange(0.5, 1);
				double theta = FastUtils.randomDoubleInRange(0, 2 * Math.PI);
				double x = r * Math.cos(theta);
				double z = r * Math.sin(theta);

				Location testLoc = spawningLoc.clone().add(x, 0, z);

				if (mob.getWorld().getBlockAt(testLoc).isPassable()) {
					spawningLoc = testLoc.clone();
					break;
				}
			}

			DelvesUtils.duplicateLibraryOfSoulsMob(mob, spawningLoc);

			Location loc = spawningLoc.add(0, mob.getHeight() + 0.5, 0);
			new PartialParticle(Particle.REDSTONE, loc, 10, 0.01, 0.3, 0.01,
				new Particle.DustOptions(Color.fromRGB(255, 255, 150), 1f)).spawnAsEnemy();
			new PartialParticle(Particle.REDSTONE, loc, 10, 0.3, 0.01, 0.01,
				new Particle.DustOptions(Color.fromRGB(255, 255, 150), 1f)).spawnAsEnemy();
		}
	}

}

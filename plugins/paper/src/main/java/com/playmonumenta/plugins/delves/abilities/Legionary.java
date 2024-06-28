package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;

public class Legionary {

	private static final double SPAWN_CHANCE_PER_LEVEL = 0.15;

	public static final String DESCRIPTION = "Enemies come in larger numbers.";

	public static Component[] rankDescription(int level) {
			return new Component[]{
				Component.text("Spawners have a " + Math.round(SPAWN_CHANCE_PER_LEVEL * level * 100) + "% chance"),
				Component.text("to spawn a copy of each enemy.")
			};
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		if (DelvesUtils.isDelveMob(mob) || EntityUtils.isBoss(mob)) {
			return;
		}

		for (int i = FastUtils.roundRandomly(SPAWN_CHANCE_PER_LEVEL * level); i > 0; i--) {
			Location spawningLoc = mob.getLocation();

			// don't spawn directly in the mob, and try 20 times to find an open spot
			double offset = Math.max(1, 0.8 * mob.getWidth());
			outer:
			for (int j = 0; j < 20; j++) {
				double r = FastUtils.randomDoubleInRange(0.5 * offset, offset);
				double theta = FastUtils.randomDoubleInRange(0, 2 * Math.PI);
				double x = r * Math.cos(theta);
				double z = r * Math.sin(theta);

				for (double y = -1; y <= 1; y += 0.5) {
					BoundingBox testBB = mob.getBoundingBox().shift(x, y, z);
					if (!LocationUtils.collidesWithBlocks(testBB, mob.getWorld(), false)) {
						spawningLoc.add(x, y, z);
						break outer;
					}
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

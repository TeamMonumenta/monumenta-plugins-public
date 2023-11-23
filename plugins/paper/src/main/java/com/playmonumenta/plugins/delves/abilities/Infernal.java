package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.EnumSet;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class Infernal {

	private static final EnumSet<DamageType> ENVIRONMENTAL_DAMAGE_CAUSES = EnumSet.of(
			DamageType.AILMENT,
			DamageType.FALL
	);

	private static final double ENVIRONMENTAL_DAMAGE_TAKEN_MULTIPLIER_PER_LEVEL = 0.1;

	private static final double BURNING_DAMAGE_TAKEN_MULTIPLIER_PER_LEVEL = 0.2;

	private static final double SPAWN_CHANCE_PER_LEVEL = 0.08;

	private static final LoSPool INFERNAL_LAND_POOL = new LoSPool.LibraryPool("~DelveInfernalLand");

	public static final String DESCRIPTION = "Fiery enemies can spawn.";

	public static String[] rankDescription(int level) {
			return new String[]{
				"Spawners have a " + Math.round(SPAWN_CHANCE_PER_LEVEL * level * 100) + "% chance to spawn Embers.",
				"Players take +" + Math.round(BURNING_DAMAGE_TAKEN_MULTIPLIER_PER_LEVEL * level * 100) + "% Burning Damage",
				"and +" + Math.round(ENVIRONMENTAL_DAMAGE_TAKEN_MULTIPLIER_PER_LEVEL * level * 100) + "% Environmental Damage."
			};
	}

	public static void applyDamageModifiers(DamageEvent event, int level) {
		if (level == 0) {
			return;
		}

		if (event.getType() == DamageType.FIRE) {
			event.setDamage(event.getDamage() * (1 + BURNING_DAMAGE_TAKEN_MULTIPLIER_PER_LEVEL * level));
		} else if (ENVIRONMENTAL_DAMAGE_CAUSES.contains(event.getType())) {
			event.setDamage(event.getDamage() * (1 + ENVIRONMENTAL_DAMAGE_TAKEN_MULTIPLIER_PER_LEVEL * level));
		}
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		if (FastUtils.RANDOM.nextDouble() < SPAWN_CHANCE_PER_LEVEL * level && !DelvesUtils.isDelveMob(mob)) {
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


				Entity entity = INFERNAL_LAND_POOL.spawn(spawningLoc);
				// Safety net in case the Infernal doesn't get summoned for some reason
				if (entity != null && mob.hasMetadata(Constants.SPAWNER_COUNT_METAKEY)) {
					/* Include the original mob's metadata for spawner counting on the Infernal. This should prevent
					   drop farming from Infernals when a death event is detected in the mob listener. */
					entity.setMetadata(Constants.SPAWNER_COUNT_METAKEY, mob.getMetadata(Constants.SPAWNER_COUNT_METAKEY).get(0));
				}

				spawningLoc.add(0, 1, 0);
				new PartialParticle(Particle.LAVA, spawningLoc, 30, 0, 0, 0, 0.1).spawnAsEnemy();
			}
		}
	}
}

package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class Infernal {

	private static final double BURNING_DAMAGE_TAKEN_MULTIPLIER_PER_LEVEL = 0.2;

	private static final double SPAWN_CHANCE_PER_LEVEL = 0.08;

	private static final LoSPool INFERNAL_LAND_POOL = new LoSPool.LibraryPool("~DelveInfernalLand");

	public static final String DESCRIPTION = "Fiery enemies can spawn.";

	public static Component[] rankDescription(int level) {
			return new Component[]{
				Component.text("For each mob a spawner produces, there is a " + Math.round(SPAWN_CHANCE_PER_LEVEL * level * 100) + "% chance to spawn an Ember."),
				Component.text("Players take +" + Math.round(BURNING_DAMAGE_TAKEN_MULTIPLIER_PER_LEVEL * level * 100) + "% Burning Damage")
			};
	}

	public static void applyDamageModifiers(DamageEvent event, int level) {
		if (level == 0) {
			return;
		}

		if (event.getType() == DamageType.FIRE) {
			event.setFlatDamage(event.getDamage() * (1 + BURNING_DAMAGE_TAKEN_MULTIPLIER_PER_LEVEL * level));
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

				spawningLoc.getWorld().playSound(spawningLoc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.HOSTILE, 1f, 0.2f);
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

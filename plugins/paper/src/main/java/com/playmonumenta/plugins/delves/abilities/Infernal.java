package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.HashSet;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;

public class Infernal {

	private static final double BURNING_DAMAGE_TAKEN_MULTIPLIER_PER_LEVEL = 0.2;

	private static final double SPAWN_CHANCE_PER_LEVEL = 0.06;

	private static final LoSPool INFERNAL_LAND_POOL = new LoSPool.LibraryPool("~DelveInfernalLand");

	public static final String DESCRIPTION = "Fiery enemies can spawn.";

	public static Component[] rankDescription(int level) {
		return new Component[]{
			Component.text("Spawners have a " + Math.round(SPAWN_CHANCE_PER_LEVEL * level * 100) + "% chance to produce an Ember."),
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

	private static final HashSet<Block> mInfernalCooldown = new HashSet<>();

	public static void applyModifiers(Block block, Entity spawnEntity, int level) {
		if (mInfernalCooldown.contains(block)) {
			return;
		}
		mInfernalCooldown.add(block);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			mInfernalCooldown.remove(block);
		}, Constants.TICKS_PER_SECOND);
		if (FastUtils.RANDOM.nextDouble() < SPAWN_CHANCE_PER_LEVEL * level) {
			Location spawningLoc = spawnEntity.getLocation().clone();

			// don't spawn directly in the mob, and try 20 times to find an open spot
			for (int j = 0; j < 20; j++) {
				double r = FastUtils.randomDoubleInRange(0.5, 1);
				double theta = FastUtils.randomDoubleInRange(0, 2 * Math.PI);
				double x = r * Math.cos(theta);
				double z = r * Math.sin(theta);

				Location testLoc = spawningLoc.clone().add(x, 0, z);
				Block spawnBlock = spawningLoc.getWorld().getBlockAt(testLoc);

				// Spawn in the center of a block with 0.3 variance
				if (spawnBlock.isPassable() && spawnBlock.getRelative(BlockFace.UP).isPassable()) {
					spawningLoc = spawnBlock.getLocation()
						.add(FastUtils.randomDoubleInRange(-0.3, 0.3) + 0.5, 0, FastUtils.randomDoubleInRange(-0.3, 0.3) + 0.5);
					break;
				}
			}
			spawningLoc.getWorld().playSound(spawningLoc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.HOSTILE, 1f, 0.2f);
			Entity entity = INFERNAL_LAND_POOL.spawn(spawningLoc);
			// Safety net in case the Infernal doesn't get summoned for some reason
			if (entity != null && block.hasMetadata(Constants.SPAWNER_COUNT_METAKEY)) {
					/* Include the original mob's metadata for spawner counting on the Infernal. This should prevent
					   drop farming from Infernals when a death event is detected in the mob listener. */
				entity.setMetadata(Constants.SPAWNER_COUNT_METAKEY, block.getMetadata(Constants.SPAWNER_COUNT_METAKEY).get(0));
			}

			spawningLoc.add(0, 1, 0);
			new PartialParticle(Particle.LAVA, spawningLoc, 30, 0, 0, 0, 0.1).spawnAsEnemy();

		}
	}
}

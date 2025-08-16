package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;

import java.util.HashSet;

public class Idolatry {

	private static final double SPAWN_CHANCE_PER_LEVEL = 0.03;
	private static final LoSPool IDOLATRY_POOL = new LoSPool.LibraryPool("~Idolatry");
	public static final String DESCRIPTION = "Spawners have a chance of spawning an immobile Idol.";

	public static Component[] rankDescription(int level) {
		// Note that the numbers here are found in IdolatryBoss.java and will NOT update live, they have to be kept in sync by hand
		return new Component[]{
			Component.text("Spawners have a " + Math.round(SPAWN_CHANCE_PER_LEVEL * level * 100) + "% chance"),
			Component.text("to produce a slow-moving Idol. While an enemy is within "),
			Component.text("a 8 block radius of an alive Idol, some of the damage it"),
			Component.text("takes will instead be redirected to the Idol."),
			Component.text("Creepers and Delve Mobs do not respect Idols.")
		};
	}

	private static final HashSet<Block> mIdolatryCooldown = new HashSet<>();
	public static void applyModifiers(Block block, int level) {
		if (FastUtils.RANDOM.nextDouble() < SPAWN_CHANCE_PER_LEVEL * level && !mIdolatryCooldown.contains(block)) {
			mIdolatryCooldown.add(block);
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
				mIdolatryCooldown.remove(block);
			}, Constants.TICKS_PER_SECOND);
			Location spawningLoc = block.getLocation().clone();
			// don't spawn directly in the mob, and try 20 times to find an open spot
			for (int j = 0; j < 20; j++) {
				double r = FastUtils.randomDoubleInRange(0.5, 1);
				double theta = FastUtils.randomDoubleInRange(0, 2 * Math.PI);
				double x = r * Math.cos(theta);
				double z = r * Math.sin(theta);

				Location testLoc = spawningLoc.clone().add(x, 0, z);

				if (block.getWorld().getBlockAt(testLoc).isPassable()) {
					spawningLoc = testLoc.clone();
					break;
				}
			}
			spawningLoc.getWorld().playSound(spawningLoc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.HOSTILE, 1.35f, 1f);
			spawningLoc.getWorld().playSound(spawningLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.HOSTILE, 0.5f, 0.80f);
			IDOLATRY_POOL.spawn(spawningLoc);
			spawningLoc.add(0, 1, 0);
			new PartialParticle(Particle.TRIAL_SPAWNER_DETECTION, spawningLoc, 30, 0, 0, 0, 0.1).spawnAsEnemy();
		}
	}
}

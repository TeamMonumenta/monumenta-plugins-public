package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTTileEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;

public class Idolatry {

	private static final double SPAWN_CHANCE_PER_LEVEL = 0.15;
	private static final LoSPool IDOLATRY_POOL = new LoSPool.LibraryPool("~Idolatry");
	public static final String DESCRIPTION = "Spawners have a chance of spawning an immobile Idol.";

	public static Component[] rankDescription(int level) {
		// Note that the numbers here are found in IdolatryBoss.java and will NOT update live, they have to be kept in sync by hand
		return new Component[]{
			Component.text("Spawners have a " + Math.round(SPAWN_CHANCE_PER_LEVEL * level * 100) + "% chance"),
			Component.text("to produce a slow-moving Idol. While an enemy is within "),
			Component.text("a 8 block radius of an alive Idol, some of the damage it"),
			Component.text("takes will instead be redirected to the Idol."),
			Component.text("Creepers and Delve Mobs do not respect Idols."),
			Component.text("Idols will slowly die without nearby mobs.")
		};
	}

	// MetaData saves by location, so use PersistentData to handle PoI respawns and whatnot.
	private static final String IDOLATRY_CHECK = "IdolatryCheck";

	public static void applyModifiers(CreatureSpawner spawner, Entity spawnEntity, int level) {
		NBTCompound persistentDataContainer = new NBTTileEntity(spawner).getPersistentDataContainer();
		if (persistentDataContainer.hasTag(IDOLATRY_CHECK)) {
			return;
		}
		persistentDataContainer.setBoolean(IDOLATRY_CHECK, true);
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
			spawningLoc.getWorld().playSound(spawningLoc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.HOSTILE, 1.35f, 1f);
			spawningLoc.getWorld().playSound(spawningLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.HOSTILE, 0.5f, 0.80f);
			IDOLATRY_POOL.spawn(spawningLoc);
			spawningLoc.add(0, 1, 0);
			new PartialParticle(Particle.TRIAL_SPAWNER_DETECTION, spawningLoc, 30, 0, 0, 0, 0.1).spawnAsEnemy();
		}
	}
}

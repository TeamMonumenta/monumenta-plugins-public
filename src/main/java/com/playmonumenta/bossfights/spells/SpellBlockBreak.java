package com.playmonumenta.bossfights.spells;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

public class SpellBlockBreak extends Spell {
	private Entity mLauncher;

	public SpellBlockBreak(Entity launcher) {
		mLauncher = launcher;
	}

	@Override
	public void run() {
		Location loc = mLauncher.getLocation();

		/* Get a list of all blocks that impede the boss's movement */
		List<Location> badBlockList = new ArrayList<Location>();
		Location testloc = new Location(loc.getWorld(), 0, 0, 0);
		for (int x = -1; x <= 1; x++) {
			testloc.setX(loc.getX() + (double)x);
			for (int y = 1; y <= 3; y++) {
				testloc.setY(loc.getY() + (double)y);
				for (int z = -1; z <= 1; z++) {
					testloc.setZ(loc.getZ() + (double)z);
					Material material = testloc.getBlock().getType();
					if (material != Material.BEDROCK && material != Material.BARRIER && (material.isSolid() || material.equals(Material.COBWEB))) {
						badBlockList.add(testloc.clone());
					}
				}
			}
		}

		/* If more than two blocks, destroy all blocking blocks */
		if (badBlockList.size() > 2) {
			for (Location targetLoc : badBlockList) {
				targetLoc.getBlock().setType(Material.AIR);
			}

			loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_PLACE, 3f, 0.6f);
			loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 3f, 0.6f);
			loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3f, 0.8f);
			Location particleLoc = loc.add(new Location(loc.getWorld(), -0.5f, 0f, 0.5f));
			particleLoc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, particleLoc, 10, 1, 1, 1, 0.03);
		}
	}

	@Override
	public int duration() {
		return 1;
	}
}

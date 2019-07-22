package com.playmonumenta.bossfights.spells;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.playmonumenta.plugins.utils.EntityUtils;

public class SpellIceBreak extends Spell {
	private Entity mLauncher;

	public SpellIceBreak(Entity launcher) {
		mLauncher = launcher;
	}

	private Random rand = new Random();
	private Set<Block> iceBlocks = new HashSet<Block>();
	private Set<Block> frostedBlocks = new HashSet<Block>();
	private Set<Block> brokenBlocks = new HashSet<Block>();

	@Override
	public void run() {
		Location loc = mLauncher.getLocation();

		// Get a list of all ice blocks around the boss.
		Location testloc = new Location(loc.getWorld(), 0, 0, 0);
		for (int x = -1; x <= 1; x++) {
			testloc.setX(loc.getX() + x);
			for (int y = -1; y <= 1; y++) {
				testloc.setY(loc.getY() + y);
				for (int z = -1; z <= 1; z++) {
					testloc.setZ(loc.getZ() + z);
					Block block = testloc.getBlock();
					Material material = block.getType();
					if (material == Material.ICE || material == Material.BLUE_ICE && material == Material.PACKED_ICE) {
						iceBlocks.add(testloc.getBlock());
					}
				}
			}
		}

		Location locationBelow = new Location(loc.getWorld(), loc.getX(), loc.getY() - 1, loc.getZ());

		// If the mob is on top of frosted ice, dont break yet.
		if (!frostedBlocks.contains(locationBelow.getBlock())) {

			// Break the frosted blocks
			for (Block block : frostedBlocks) {
				// Dont break a block when its right around an icebreak boss's feet.
				boolean Break = true;
				for (LivingEntity mob : EntityUtils.getNearbyMobs(block.getLocation(), 1.2)) {
					if (mob.getScoreboardTags().contains("boss_icebreak")) {
						Break = false;
						break;
					}
				}
				if (Break) {
					loc.getWorld().playSound(block.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.3f, 0.9f);
					block.getLocation().getWorld().spawnParticle(Particle.WATER_SPLASH, block.getLocation(), 10, 1, 1, 1, 0.03);
					block.setType(Material.AIR);
					brokenBlocks.add(block);
				}
			}
			for (Block block : brokenBlocks) {
				frostedBlocks.remove(block);
			}

			// Make the new frosted blocks
			for (Block block : iceBlocks) {
				// Randomise a bit to make it look better.
				if (rand.nextDouble() < 0.6 || block == locationBelow.getBlock()) {
					loc.getWorld().playSound(block.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.1f, 0.9f);
					block.getLocation().getWorld().spawnParticle(Particle.CLOUD, block.getLocation(), 10, 1, 1, 1, 0.03);
					block.setType(Material.FROSTED_ICE);
					frostedBlocks.add(block);
				}
			}
		}

		iceBlocks.clear();
	}

	@Override
	public int duration() {
		return 1;
	}
}

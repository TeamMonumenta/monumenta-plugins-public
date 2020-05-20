package com.playmonumenta.plugins.bosses.spells;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;

public class SpellIceBreak extends Spell {
	private Entity mLauncher;

	public SpellIceBreak(Entity launcher) {
		mLauncher = launcher;
	}

	private Set<Block> mIceBlocks = new HashSet<Block>();
	private Set<Block> mFrostedBlocks = new HashSet<Block>();
	private Set<Block> mBrokenBlocks = new HashSet<Block>();

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
					if (material == Material.ICE || material == Material.BLUE_ICE || material == Material.PACKED_ICE) {
						mIceBlocks.add(testloc.getBlock());
					}
				}
			}
		}

		Location locationBelow = new Location(loc.getWorld(), loc.getX(), loc.getY() - 1, loc.getZ());

		// If the mob is on top of frosted ice, dont break yet.
		if (!mFrostedBlocks.contains(locationBelow.getBlock())) {

			// Break the frosted blocks
			for (Block block : mFrostedBlocks) {
				// Dont break a block when its right around an icebreak boss's feet.
				boolean shouldBreak = true;
				for (LivingEntity mob : EntityUtils.getNearbyMobs(block.getLocation(), 1.2)) {
					if (mob.getScoreboardTags().contains("boss_icebreak")) {
						shouldBreak = false;
						break;
					}
				}
				if (shouldBreak) {
					loc.getWorld().playSound(block.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.3f, 0.9f);
					block.getLocation().getWorld().spawnParticle(Particle.WATER_SPLASH, block.getLocation(), 10, 1, 1, 1, 0.03);
					block.setType(Material.AIR);
					mBrokenBlocks.add(block);
				}
			}
			for (Block block : mBrokenBlocks) {
				mFrostedBlocks.remove(block);
			}
			mBrokenBlocks.clear();

			// Make the new frosted blocks
			for (Block block : mIceBlocks) {
				// Randomise a bit to make it look better.
				if (FastUtils.RANDOM.nextDouble() < 0.6 || block == locationBelow.getBlock()) {
					loc.getWorld().playSound(block.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.1f, 0.9f);
					block.getLocation().getWorld().spawnParticle(Particle.CLOUD, block.getLocation(), 10, 1, 1, 1, 0.03);
					block.setType(Material.FROSTED_ICE);
					mFrostedBlocks.add(block);
				}
			}
		}
		mIceBlocks.clear();
	}

	@Override
	public int duration() {
		return 1;
	}
}

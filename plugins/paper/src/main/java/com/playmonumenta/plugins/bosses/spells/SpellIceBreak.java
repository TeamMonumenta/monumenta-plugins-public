package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class SpellIceBreak extends Spell {
	private final Entity mLauncher;
	private List<Block> mIceBlocks = new ArrayList<>();
	private final List<Block> mFrostedBlocks = new ArrayList<>();
	private final List<Block> mBrokenBlocks = new ArrayList<>();

	public SpellIceBreak(Entity launcher) {
		mLauncher = launcher;
	}

	@Override
	public void run() {
		Location loc = mLauncher.getLocation();

		// Get a list of all ice blocks around the boss.
		mIceBlocks = BlockUtils.getBlocksInCube(loc, 1);
		mIceBlocks.removeIf(block -> block.getType() != Material.ICE
			&& block.getType() != Material.BLUE_ICE
			&& block.getType() != Material.PACKED_ICE);

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
					loc.getWorld().playSound(block.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 0.3f, 0.9f);
					new PartialParticle(Particle.WATER_SPLASH, block.getLocation(), 10, 1, 1, 1, 0.03).spawnAsEntityActive(mLauncher);
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
					loc.getWorld().playSound(block.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 0.1f, 0.9f);
					new PartialParticle(Particle.CLOUD, block.getLocation(), 10, 1, 1, 1, 0.03).spawnAsEntityActive(mLauncher);
					block.setType(Material.FROSTED_ICE);
					mFrostedBlocks.add(block);
				}
			}
		}
		mIceBlocks.clear();
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}
}

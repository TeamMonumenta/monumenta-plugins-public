package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.effects.BaseMovementSpeedModifyEffect;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.HashSet;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class SpellChangeFloor extends Spell {
	private static final String SLOWNESS_SRC = "SpellChangeFloorSelfSlow";

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenterLoc;
	private final int mRange;
	private final int mRadius;
	private final Material mMaterial;
	private final int mFloorDuration;

	public SpellChangeFloor(final Plugin plugin, final LivingEntity launcher, final Location centerLoc, final int range,
							final int radius, final Material material, final int floorDuration) {
		mPlugin = plugin;
		mBoss = launcher;
		mCenterLoc = centerLoc;
		mRange = range;
		mRadius = radius;
		mMaterial = material;
		mFloorDuration = floorDuration;
	}

	@Override
	public void run() {
		final List<Player> players = PlayerUtils.playersInRange(mCenterLoc, mRange, true);
		if (!players.isEmpty()) {
			launch(players.get(FastUtils.RANDOM.nextInt(players.size())).getLocation());
		}
	}

	@Override
	public int cooldownTicks() {
		return Constants.TICKS_PER_SECOND * 8;
	}

	private void launch(final Location targetLoc) {
		final HashSet<Block> changedBlocks = new HashSet<>();

		com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, SLOWNESS_SRC,
			new BaseMovementSpeedModifyEffect(30, -0.6));
		mBoss.getWorld().playSound(targetLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1f, 2f);
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1f, 2f);
		new PartialParticle(Particle.LAVA, mBoss.getLocation(), 5, 0.8, 0.8, 0.8, 0).spawnAsEntityActive(mBoss);

		final Location blockLoc = targetLoc.clone();
		for (double dx = targetLoc.getX() - mRadius; dx <= targetLoc.getX() + mRadius; dx++) {
			for (double dy = targetLoc.getY() - mRadius; dy <= targetLoc.getY() + mRadius; dy++) {
				for (double dz = targetLoc.getZ() - mRadius; dz <= targetLoc.getZ() + mRadius; dz++) {
					final Block block = blockLoc.set(dx, dy, dz).getBlock();
					/* Special case for obsidian because of Azacor's arena */
					if (FastUtils.RANDOM.nextInt(16) > 6 && !(BlockUtils.isMechanicalBlock(block.getType()) || block.getType() == Material.OBSIDIAN)) {
						changedBlocks.add(block);
					}
				}
			}
		}

		final BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				for (final Block block : changedBlocks) {
					final Location loc = block.getLocation().add(0.5f, 0.5f, 0.5f);
					new PartialParticle(Particle.DRAGON_BREATH, loc, 4, 0.3, 0.3, 0.3, 0).spawnAsEntityActive(mBoss);
				}

				/* Set the blocks to the specified material */
				if (mTicks == Constants.TICKS_PER_SECOND * 3) {
					changedBlocks.forEach(block -> TemporaryBlockChangeManager.INSTANCE.changeBlock(block, mMaterial, mFloorDuration));
				}

				/* The particles are intentionally canceled 60 ticks early as an indicator that the floor will change back */
				if (mTicks >= mFloorDuration) {
					this.cancel();
					return;
				}

				mTicks += 5;
			}
		};

		runnable.runTaskTimer(mPlugin, 0, 5);
		mActiveRunnables.add(runnable);
	}
}

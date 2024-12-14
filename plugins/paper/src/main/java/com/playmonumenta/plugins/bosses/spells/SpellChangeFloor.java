package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.particle.PartialParticle;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public final class SpellChangeFloor extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenterLoc;
	private final int mRange;
	private final int mRadius;
	private final Material mMaterial;
	private final int mFloorDuration;

	public SpellChangeFloor(final Plugin plugin, final LivingEntity launcher, final Location centerLoc, final int range,
							final int radius, final Material material, final int floorduration) {
		mPlugin = plugin;
		mBoss = launcher;
		mCenterLoc = centerLoc;
		mRange = range;
		mRadius = radius;
		mMaterial = material;
		mFloorDuration = floorduration;
	}

	@Override
	public void run() {
		final List<Player> players = PlayerUtils.playersInRange(mCenterLoc, mRange, true);
		if (!players.isEmpty()) {
			launch(players.get(FastUtils.RANDOM.nextInt(players.size())));
		}
	}

	@Override
	public int cooldownTicks() {
		return Constants.TICKS_PER_SECOND * 8;
	}

	private void launch(final Player target) {
		final HashSet<Block> changedBlocks = new HashSet<>();

		target.playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1f, 4f);
		mBoss.getLocation().getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1f, 5f);
		new PartialParticle(Particle.LAVA, mBoss.getLocation(), 1, 0.8, 0.8, 0.8, 0).spawnAsEntityActive(mBoss);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 30, 3));

		// Get a list of blocks that should be changed
		for (int dx = -mRadius; dx < mRadius; dx++) {
			for (int dy = -mRadius; dy < mRadius; dy++) {
				for (int dz = -mRadius; dz < mRadius; dz++) {
					final Block block = target.getLocation().add(dx, dy, dz).getBlock();
					if (FastUtils.RANDOM.nextInt(16) > 6) {
						changedBlocks.add(block);
					}
				}
			}
		}

		final BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				if (mTicks < mFloorDuration) {
					// Particles over the changed blocks
					for (final Block block : changedBlocks) {
						final Location loc = block.getLocation().add(0.5f, 1f, 0.5f);
						new PartialParticle(Particle.DRAGON_BREATH, loc, 1, 0.3, 0.3, 0.3, 0).spawnAsEntityActive(mBoss);
					}

					return;
				}

				// Set the blocks to the specified material
				changedBlocks.forEach(block -> TemporaryBlockChangeManager.INSTANCE.changeBlock(block, mMaterial, mFloorDuration));
				this.cancel();
			}
		};

		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}
}

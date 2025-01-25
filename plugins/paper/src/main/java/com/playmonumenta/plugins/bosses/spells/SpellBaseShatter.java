package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellBaseShatter extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final double mRadius;
	private final int mCooldown;
	private final int mDelay;
	private final int mNumLines;
	private final double mHeight;
	private final double mForcedYHeightIndicator;
	private final Spell.GetSpellTargets<LivingEntity> mSpellTargets;
	private final Material mIndicator;
	private final HitAction mHitAction;
	private final StartAesthetics mStartAesthetics;
	private final WarningAesthetics mWarningAesthetics;
	private final LaunchAesthetics mLaunchAesthetics;

	private final List<Block> mChangedBlocks = new ArrayList<>();

	public SpellBaseShatter(final Plugin plugin, final LivingEntity boss, final double radius, final int cooldown,
		final int delay, final int numLines, final double height, final Material indicator,
		final GetSpellTargets<LivingEntity> spellTargets, final WarningAesthetics warningAesthetics,
		final LaunchAesthetics launchAesthetics, final HitAction hitAction) {
		this(plugin, boss, radius, cooldown, delay, numLines, height, -65, indicator, spellTargets,
			(final LivingEntity launcher) -> { }, warningAesthetics, launchAesthetics, hitAction);
	}

	public SpellBaseShatter(final Plugin plugin, final LivingEntity boss, final double radius, final int cooldown,
		final int delay, final int numLines, final double height, final double forcedYHeightIndicator, final Material indicator,
		final GetSpellTargets<LivingEntity> spellTargets, final StartAesthetics startAesthetics,
		final WarningAesthetics warningAesthetics, final LaunchAesthetics launchAesthetics, final HitAction hitAction) {
		mPlugin = plugin;
		mBoss = boss;
		mRadius = radius;
		mCooldown = cooldown;
		mDelay = delay;
		mNumLines = numLines;
		mHeight = height;
		mForcedYHeightIndicator = forcedYHeightIndicator;
		mSpellTargets = spellTargets;
		mIndicator = indicator;
		mStartAesthetics = startAesthetics;
		mWarningAesthetics = warningAesthetics;
		mLaunchAesthetics = launchAesthetics;
		mHitAction = hitAction;
	}

	@Override
	public void run() {
		final List<? extends LivingEntity> targets = mSpellTargets.getTargets();
		if (targets.isEmpty()) {
			return;
		}

		final LivingEntity target = targets.size() > 1 ? targets.get(FastUtils.RANDOM.nextInt(targets.size())) : targets.get(0);
		Vector facingDirection = LocationUtils.getDirectionTo(target.getLocation(), mBoss.getLocation()).setY(0).normalize();
		if (!Double.isFinite(facingDirection.getX())) {
			facingDirection = new Vector(0, 1, 0);
		}
		mBoss.teleport(mBoss.getLocation().setDirection(facingDirection));
		final Location bossLoc = mBoss.getLocation();
		EntityUtils.selfRoot(mBoss, mDelay);
		mStartAesthetics.launch(mBoss);

		final BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;
			float mPitch = 0;

			@Override
			public void run() {
				if (EntityUtils.shouldCancelSpells(mBoss)) {
					SpellBaseShatter.this.cancel();
					EntityUtils.cancelSelfRoot(mBoss);
					return;
				}

				mT += 2;
				mPitch += 0.025f;

				if (mT % 2 == 0) {
					mWarningAesthetics.launch(mBoss, mBoss.getLocation(), mPitch);
				}

				//Every half-second, do visuals
				if (mT % (mDelay / 5) == 0) {
					final double angleSplit = (double) 360 / (2 * mNumLines);

					for (double r = 0; r < mRadius; r += 0.5) {
						for (double dir = 0; dir <= 360 - angleSplit * 2; dir += angleSplit * 2) {
							Vector vec;
							final double resolution = 45.0 / (r + 1.0);
							for (double degree = -angleSplit / 2; degree < angleSplit / 2; degree += resolution) {
								vec = new Vector(FastUtils.cosDeg(degree) * r, 0, FastUtils.sinDeg(degree) * r);
								vec = VectorUtils.rotateYAxis(vec, bossLoc.getYaw() + dir + 90);

								final Location testLoc = bossLoc.clone().add(vec);
								if (mForcedYHeightIndicator > testLoc.getWorld().getMinHeight()) {
									testLoc.setY(mForcedYHeightIndicator);
								} else {
									testLoc.add(0, -1, 0);
								}

								/* Spawns warning indicator block as a warning at a 1/3 rate, will try to climb 1 block up or down if needed */
								if (testLoc.getBlock().getType() != mIndicator) {
									if (FastUtils.RANDOM.nextInt(3) == 0 || mT >= 3 * mDelay / 4) {
										while (testLoc.getBlock().getRelative(BlockFace.UP).getType().isSolid() && testLoc.getBlockY() <= bossLoc.getBlockY() + 3) {
											testLoc.add(0, 1, 0);
										}
										if (testLoc.getBlock().getType() == Material.AIR && testLoc.getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) {
											testLoc.subtract(0, 1, 0);
										}
										//Move up one block if on barrier or bedrock level
										if (BlockUtils.isMechanicalBlock(testLoc.getBlock().getType()) || BlockUtils.isValuableBlock(testLoc.getBlock().getType()) || BlockUtils.isNonEmptyContainer(testLoc.getBlock())) {
											testLoc.add(0, 1, 0);
										}
										if (testLoc.getBlock().getType() != mIndicator && testLoc.getBlock().getType().isSolid()) {
											if (TemporaryBlockChangeManager.INSTANCE.changeBlock(testLoc.getBlock(), mIndicator, mDelay + FastUtils.randomIntInRange(0, 10))) {
												mChangedBlocks.add(testLoc.getBlock());
											}
										}
									}
								}
							}
						}
					}
				}

				/* End shatter and perform hit actions on hit players */
				if (mT >= mDelay) {
					EntityUtils.cancelSelfRoot(mBoss);
					this.cancel();
					mLaunchAesthetics.launch(mBoss, mBoss.getLocation());

					final List<Player> hitPlayers = new ArrayList<>();
					Hitbox.ApproximateFreeformHitbox hitboxCylSeg;
					final Location bossLocFacingDirection = bossLoc.clone();

					/* Split an imaginary cylinder of radius mRadius and height mHeight into mNumLines * 2 segments */
					final double angleSplit = (double) 360 / (2 * mNumLines);
					for (double dir = 0; dir <= 360 - angleSplit * 2; dir += angleSplit * 2) {
						bossLocFacingDirection.setYaw((float) (bossLoc.getYaw() + dir));
						hitboxCylSeg = Hitbox.ApproximateFreeformHitbox.approximateCylinderSegment(bossLocFacingDirection, mHeight, mRadius, Math.toRadians(angleSplit / 2.0));
						hitPlayers.addAll(hitboxCylSeg.getHitPlayers(true));
					}

					hitPlayers.forEach(player -> mHitAction.launch(mBoss, player, player.getLocation()));
				}
			}
		};

		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}

	@Override
	public void cancel() {
		super.cancel();

		TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(mChangedBlocks, mIndicator);
		mChangedBlocks.clear();
	}

	@FunctionalInterface
	public interface StartAesthetics {
		void launch(LivingEntity boss);
	}

	@FunctionalInterface
	public interface WarningAesthetics {
		void launch(LivingEntity boss, Location loc, float soundPitch);
	}

	@FunctionalInterface
	public interface LaunchAesthetics {
		void launch(LivingEntity boss, Location loc);
	}

	@FunctionalInterface
	public interface HitAction {
		void launch(LivingEntity boss, LivingEntity target, Location location);
	}
}

package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
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
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellBaseShatter extends Spell {
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private double mRadius;
	private int mCooldown;
	private int mDelay;
	private int mNumLines;
	private final double mHeight;
	private Spell.GetSpellTargets<LivingEntity> mSpellTargets;
	private Material mIndicator;
	private final HitAction mHitAction;
	private final WarningAesthetics mWarningAesthetics;
	private final LaunchAesthetics mLaunchAesthetics;

	private final List<Block> mChangedBlocks = new ArrayList<>();

	public SpellBaseShatter(
		Plugin plugin,
		LivingEntity boss,
		double radius,
		int cooldown,
		int delay,
		int numLines,
		Material indicator,
		GetSpellTargets<LivingEntity> spellTargets,
		WarningAesthetics warningAesthetics,
		LaunchAesthetics launchAesthetics,
		HitAction hitAction
	) {
		mPlugin = plugin;
		mBoss = boss;
		mRadius = radius;
		mCooldown = cooldown;
		mDelay = delay;
		mNumLines = numLines;
		mHeight = 15;
		mSpellTargets = spellTargets;
		mIndicator = indicator;
		mWarningAesthetics = warningAesthetics;
		mLaunchAesthetics = launchAesthetics;
		mHitAction = hitAction;
	}

	public SpellBaseShatter(
			Plugin plugin,
			LivingEntity boss,
			double radius,
			int cooldown,
			int delay,
			int numLines,
			double height,
			Material indicator,
			GetSpellTargets<LivingEntity> spellTargets,
			WarningAesthetics warningAesthetics,
			LaunchAesthetics launchAesthetics,
			HitAction hitAction
	) {
		mPlugin = plugin;
		mBoss = boss;
		mRadius = radius;
		mCooldown = cooldown;
		mDelay = delay;
		mNumLines = numLines;
		mHeight = height;
		mSpellTargets = spellTargets;
		mIndicator = indicator;
		mWarningAesthetics = warningAesthetics;
		mLaunchAesthetics = launchAesthetics;
		mHitAction = hitAction;
	}

	@Override
	public void run() {
		mBoss.setAI(false);
		List<? extends LivingEntity> targets = mSpellTargets.getTargets();

		LivingEntity target = null;
		//Choose random target
		if (targets.size() == 1) {
			target = targets.get(0);
		} else if (targets.size() > 1) {
			target = targets.get(FastUtils.RANDOM.nextInt(targets.size()));
		}

		if (target != null) {
			Vector dir = LocationUtils.getDirectionTo(target.getLocation(), mBoss.getLocation()).setY(0).normalize();
			if (!Double.isFinite(dir.getX())) {
				dir = new Vector(0, 1, 0);
			}
			mBoss.teleport(mBoss.getLocation().setDirection(dir));
		}

		Location loc = mBoss.getLocation();

		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;
			float mPitch = 0;

			@Override
			public void run() {
				mT += 2;
				mPitch += 0.025f;

				//Play shatter sound
				if (mT % 2 == 0) {
					mWarningAesthetics.launch(mBoss, mBoss.getLocation(), mPitch);
				}

				//Every half-second, do visuals
				if (mT % (mDelay / 5) == 0) {
					double angleSplit = (double) 360 / (2 * mNumLines);

					for (double r = 0; r < mRadius; r += 0.5) {
						for (double dir = 0; dir <= 360 - angleSplit * 2; dir += angleSplit * 2) {
							Vector vec;
							double resolution = 45.0 / (r + 1.0);
							for (double degree = -angleSplit / 2; degree < angleSplit / 2; degree += resolution) {
								double radian1 = Math.toRadians(degree);
								vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
								vec = VectorUtils.rotateYAxis(vec, loc.getYaw() + dir + 90);

								//Spawns particles
								Location l = loc.clone().add(vec);

								l.subtract(0, 1, 0);
								//Spawns crimson hyphae as a warning at a 1/3 rate, will try to climb 1 block up or down if needed
								if (l.getBlock().getType() != mIndicator) {
									if (FastUtils.RANDOM.nextInt(3) == 0 || mT >= 3 * mDelay / 4) {
										while (l.getBlock().getRelative(BlockFace.UP).getType().isSolid() && l.getBlockY() <= loc.getBlockY() + 3) {
											l.add(0, 1, 0);
										}
										if (l.getBlock().getType() == Material.AIR && l.getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) {
											l.subtract(0, 1, 0);
										}
										//Move up one block if on barrier or bedrock level
										if (BlockUtils.isMechanicalBlock(l.getBlock().getType()) || BlockUtils.isValuableBlock(l.getBlock().getType()) || BlockUtils.isNonEmptyContainer(l.getBlock())) {
											l.add(0, 1, 0);
										}
										if (l.getBlock().getType() != mIndicator && l.getBlock().getType().isSolid()) {
											if (TemporaryBlockChangeManager.INSTANCE.changeBlock(l.getBlock(), mIndicator, mDelay + FastUtils.randomIntInRange(0, 10))) {
												mChangedBlocks.add(l.getBlock());
											}
										}
									}
								}
							}
						}
					}
				}

				//End shatter, deal damage, show visuals
				if (mT >= mDelay) {
					mBoss.setAI(true);
					this.cancel();
					mLaunchAesthetics.launch(mBoss, mBoss.getLocation());
					Vector vec;
					List<BoundingBox> boxes = new ArrayList<>();

					double angleSplit = (double) 360 / (2 * mNumLines);
					for (double r = 0; r < mRadius; r += 0.5) {
						double resolution = 45.0 / (r + 1.0);
						for (double dir = 0; dir <= 360 - angleSplit * 2; dir += angleSplit * 2) {
							for (double degree = -angleSplit / 2; degree < angleSplit / 2; degree += resolution) {
								double radian1 = Math.toRadians(degree);
								vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
								vec = VectorUtils.rotateYAxis(vec, loc.getYaw() + dir + 90);

								Location l = loc.clone().add(vec);
								//1.5 -> 15
								BoundingBox box = BoundingBox.of(l, 0.65, mHeight, 0.65);
								boxes.add(box);
							}
						}
					}

					for (Player player : PlayerUtils.playersInRange(loc, mRadius + 20, true)) {
						List<Player> hitPlayers = new ArrayList<>();
						for (BoundingBox box : boxes) {
							if (player.getBoundingBox().overlaps(box) && !hitPlayers.contains(player)) {
								mHitAction.launch(mBoss, player, player.getLocation());
								hitPlayers.add(player);
							}
						}
					}
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

package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.bosses.MageCosmicMoonbladeBoss;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellMageCosmicMoonblade extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final MageCosmicMoonbladeBoss.Parameters mParams;
	private boolean mTelegraphInterrupt;

	public SpellMageCosmicMoonblade(Plugin plugin, LivingEntity boss, MageCosmicMoonbladeBoss.Parameters p) {
		mPlugin = plugin;
		mBoss = boss;
		mParams = p;
		mTelegraphInterrupt = false;
	}

	@Override
	public void run() {
		boolean hasGlowing = mBoss.isGlowing();
		mBoss.setGlowing(true);
		List<? extends LivingEntity> targets = mParams.TARGETS_DIRECTION.getTargetsList(mBoss);

		if (!targets.isEmpty()) {
			Location bossEyeLoc = mBoss.getEyeLocation();
			Location targetLoc = targets.get(0).getLocation().clone().add(0, 1.8, 0);
			Vector targetDirection = targetLoc.toVector().subtract(bossEyeLoc.toVector()).normalize();
			Location originTargetDirection = mBoss.getLocation().clone().setDirection(targetDirection);

			mParams.SOUND_TELL.play(mBoss.getLocation());

			mTelegraphInterrupt = false;
			new BukkitRunnable() {
				int mDegree = mParams.START_ANGLE;

				@Override
				public void run() {
					if (EntityUtils.shouldCancelSpells(mBoss)) {
						cancel();
						mTelegraphInterrupt = true;
						return;
					}

					for (double r = 1; r < mParams.RANGE; r += 0.5) {
						for (double degree = mDegree; degree < mDegree + mParams.DEGREE_INCREMENT; degree += 5) {
							double radian1 = Math.toRadians(degree);
							Vector vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
							vec = VectorUtils.rotateZAxis(vec, -8);
							vec = VectorUtils.rotateXAxis(vec, originTargetDirection.getPitch());
							vec = VectorUtils.rotateYAxis(vec, originTargetDirection.getYaw());

							Location l = originTargetDirection.clone().add(0, 1.25, 0).add(vec);
							mParams.PARTICLE_TELL.spawn(mBoss, l);
						}
					}
					if (mDegree >= mParams.END_ANGLE) {
						cancel();
						return;
					}
					mDegree += mParams.DEGREE_INCREMENT;
				}
			}.runTaskTimer(mPlugin, 0, 1);

			if (mTelegraphInterrupt) {
				return;
			}

			new BukkitRunnable() {
				final Vector mTargetDirection = targetDirection;
				int mTimes = 0;
				float mPitch = 1.2f;
				int mSwings = 0;

				@Override
				public void run() {
					if (mTimes == 0 && !hasGlowing) {
						mBoss.setGlowing(false);
					}

					if (EntityUtils.shouldCancelSpells(mBoss)) {
						cancel();
						return;
					}
					mTimes++;
					mSwings++;
					Location origin = mBoss.getLocation().clone();
					Location originTargetDirection = origin.setDirection(mTargetDirection);
					List<? extends LivingEntity> targets = mParams.TARGETS.getTargetsList(mBoss);

					if (mTimes >= mParams.SWINGS) {
						mPitch = 1.45f;
					}
					mParams.SOUND_SWING.play(originTargetDirection, 1, mPitch);

					new BukkitRunnable() {
						final int mI = mSwings;
						double mRoll;
						double mD = mParams.START_ANGLE;
						boolean mInit = false;
						final List<LivingEntity> mAlreadyHit = new ArrayList<>();

						@Override
						public void run() {
							if (!mInit) {
								if (mI % 2 == 0) {
									mRoll = -8;
									mD = mParams.START_ANGLE;
								} else {
									mRoll = 8;
									mD = mParams.END_ANGLE;
								}
								mInit = true;
							}
							if (EntityUtils.shouldCancelSpells(mBoss)) {
								cancel();
								return;
							}
							if (mI % 2 == 0) {
								Vector vec;
								for (double r = 1; r < mParams.RANGE; r += 0.5) {
									for (double degree = mD; degree < mD + mParams.DEGREE_INCREMENT; degree += 5) {
										double radian1 = Math.toRadians(degree);
										vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
										vec = VectorUtils.rotateZAxis(vec, mRoll);
										vec = VectorUtils.rotateXAxis(vec, originTargetDirection.getPitch());
										vec = VectorUtils.rotateYAxis(vec, originTargetDirection.getYaw());

										Location l = originTargetDirection.clone().add(0, 1.25, 0).add(vec);
										mParams.PARTICLE_SWING.spawn(mBoss, l);

										BoundingBox box = BoundingBox.of(l, 0.3, 1, 0.3);

										for (LivingEntity target : targets) {
											if (target.getBoundingBox().overlaps(box) && !mAlreadyHit.contains(target)) {
												damageAction(target);
												mAlreadyHit.add(target);
											}
										}
									}
								}

								mD += mParams.DEGREE_INCREMENT;
							} else {
								Vector vec;
								for (double r = 1; r < mParams.RANGE; r += 0.5) {
									for (double degree = mD; degree > mD - mParams.DEGREE_INCREMENT; degree -= 5) {
										double radian1 = Math.toRadians(degree);
										vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
										vec = VectorUtils.rotateZAxis(vec, mRoll);
										vec = VectorUtils.rotateXAxis(vec, originTargetDirection.getPitch());
										vec = VectorUtils.rotateYAxis(vec, originTargetDirection.getYaw());

										Location l = originTargetDirection.clone().add(0, 1.25, 0).add(vec);
										l.setPitch(-l.getPitch());
										mParams.PARTICLE_SWING.spawn(mBoss, l);

										BoundingBox box = BoundingBox.of(l, 0.3, 1, 0.3);

										for (LivingEntity target : targets) {
											if (target.getBoundingBox().overlaps(box) && !mAlreadyHit.contains(target)) {
												damageAction(target);
												mAlreadyHit.add(target);
											}
										}
									}
								}
								mD -= mParams.DEGREE_INCREMENT;
							}

							if ((mD >= mParams.END_ANGLE && mI % 2 == 0) || (mD <= mParams.START_ANGLE && mI % 2 > 0)) {
								mAlreadyHit.clear();
								this.cancel();
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
					if (mTimes >= mParams.SWINGS) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, mParams.SPELL_DELAY, mParams.SWINGS_DELAY);
		}
	}

	public void damageAction(LivingEntity target) {
		if (mParams.DAMAGE > 0) {
			BossUtils.blockableDamage(mBoss, target, DamageEvent.DamageType.MAGIC, mParams.DAMAGE, mParams.SPELL_NAME, mBoss.getLocation(), mParams.EFFECTS.mEffectList());
		}

		if (mParams.DAMAGE_PERCENTAGE > 0.0) {
			BossUtils.bossDamagePercent(mBoss, target, mParams.DAMAGE_PERCENTAGE, mBoss.getLocation(), mParams.SPELL_NAME, mParams.EFFECTS.mEffectList());
		}

		mParams.EFFECTS.apply(target, mBoss);
	}

	@Override
	public boolean canRun() {
		return !mParams.TARGETS_DIRECTION.getTargetsList(mBoss).isEmpty();
	}

	@Override
	public int cooldownTicks() {
		return mParams.COOLDOWN;
	}
}

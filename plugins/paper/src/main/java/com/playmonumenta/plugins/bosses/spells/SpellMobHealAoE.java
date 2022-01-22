package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class SpellMobHealAoE extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mCooldown;
	private final int mDuration;
	private final double mParticleRadius;
	private final boolean mCanMoveWhileCasting;
	private final GetSpellTargets<LivingEntity> mGetSpellTargets;
	private final Aesthetic mChargeAuraAesthetic;
	private final Aesthetic mChargeCircleAesthetic;
	private final Aesthetic mOutburstAreaAesthetic;
	private final Aesthetic mOutburstCircleAesthetic;
	private final HealingAction mHealingAction;


	public SpellMobHealAoE(
		Plugin plugin,
		LivingEntity boss,
		int cooldown,
		int duration,
		double particleRadius,
		boolean canMoveWhileCasting,
		GetSpellTargets<LivingEntity> getSpellTargets,
		Aesthetic chargeAuraAesthetic,
		Aesthetic chargeCircleAesthetic,
		Aesthetic outburstAreaAesthetic,
		Aesthetic outburstCircleAesthetic,
		HealingAction healingAction
	) {
		mPlugin = plugin;
		mBoss = boss;
		mCooldown = cooldown;
		mDuration = duration;
		mParticleRadius = particleRadius;
		mCanMoveWhileCasting = canMoveWhileCasting;
		mGetSpellTargets = getSpellTargets;
		mChargeAuraAesthetic = chargeAuraAesthetic;
		mChargeCircleAesthetic = chargeCircleAesthetic;
		mOutburstAreaAesthetic = outburstAreaAesthetic;
		mOutburstCircleAesthetic = outburstCircleAesthetic;
		mHealingAction = healingAction;
	}

	@Override
	public void run() {
		if (!mCanMoveWhileCasting) {
			mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, mDuration, 20));
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTimer = 0;
			double mCurrentRadius = mParticleRadius;
			@Override
			public void run() {
				Location loc = mBoss.getLocation();
				if (!mBoss.isValid() || mBoss.isDead()) {
					this.cancel();
					return;
				}

				mChargeAuraAesthetic.run(loc.clone().add(0, 1, 0), mTimer);

				for (double i = 0; i < 360; i += 30) {
					double radian1 = Math.toRadians(i);
					loc.add(FastUtils.cos(radian1) * mCurrentRadius, 0, FastUtils.sin(radian1) * mCurrentRadius);
					mChargeCircleAesthetic.run(loc, mTimer);
					loc.subtract(FastUtils.cos(radian1) * mCurrentRadius, 0, FastUtils.sin(radian1) * mCurrentRadius);
				}
				mCurrentRadius -= (mParticleRadius / ((double) mDuration));
				if (mCurrentRadius <= 0) {
					this.cancel();
					List<? extends LivingEntity> targets = mGetSpellTargets.getTargets();
					for (LivingEntity target : targets) {
						mHealingAction.run(target);
					}
					mOutburstAreaAesthetic.run(mBoss.getLocation(), mTimer);

					//Aesthetics for boss
					new BukkitRunnable() {
						final Location mLoc = mBoss.getLocation();
						double mBurstRadius = 0;
						@Override
						public void run() {
							for (int j = 0; j < 2; j++) {
								mBurstRadius += 1.5;
								for (double i = 0; i < 360; i += 15) {
									double radian1 = Math.toRadians(i);
									mLoc.add(FastUtils.cos(radian1) * mBurstRadius, 0, FastUtils.sin(radian1) * mBurstRadius);
									mOutburstCircleAesthetic.run(mLoc, 0);
									mLoc.subtract(FastUtils.cos(radian1) * mBurstRadius, 0, FastUtils.sin(radian1) * mBurstRadius);
								}
							}
							if (mBurstRadius >= mParticleRadius) {
								this.cancel();
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
				}

				if (mTimer > mDuration) {
					this.cancel();
				}

				mTimer++;
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();
				mActiveRunnables.remove(this);
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(mPlugin, 0, 1);

	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}

	@Override
	public boolean canRun() {
		return shouldHeal();
	}

	private boolean shouldHeal() {
		int healers = 1;
		for (LivingEntity e : mGetSpellTargets.getTargets()) {
			if (e.getScoreboardTags().contains("boss_rejuvenation")) {
				healers++;
			}
		}
		return !(FastUtils.RANDOM.nextDouble() > 1.0 / Math.pow(healers, 2));
	}

	@FunctionalInterface
	public interface Aesthetic {
		void run(Location loc, int ticks);
	}

	@FunctionalInterface
	public interface HealingAction {
		void run(LivingEntity target);
	}
}

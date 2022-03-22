package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;


public abstract class SpellBaseAoE extends Spell {

	protected final Plugin mPlugin;
	protected final LivingEntity mLauncher;
	protected final int mRadius;
	protected final int mDuration;
	protected final int mCooldown;
	protected final boolean mCanMoveWhileCasting;
	protected final Sound mChargeSound;
	protected final float mSoundVolume;
	protected final int mSoundDensity;
	protected final boolean mLineOfSight;

	public SpellBaseAoE(Plugin plugin, LivingEntity launcher, int radius, int duration, int cooldown, boolean canMoveWhileCasting, boolean needLineOfSight,
	                    Sound chargeSound) {
		this(plugin, launcher, radius, duration, cooldown, canMoveWhileCasting, needLineOfSight, chargeSound, 1f, 1);
	}

	public SpellBaseAoE(Plugin plugin, LivingEntity launcher, int radius, int duration, int cooldown, boolean canMoveWhileCasting,
	                    Sound chargeSound) {
		this(plugin, launcher, radius, duration, cooldown, canMoveWhileCasting, chargeSound, 1f, 1);
	}

	public SpellBaseAoE(Plugin plugin, LivingEntity launcher, int radius, int duration, int cooldown, boolean canMoveWhileCasting,
	                    Sound chargeSound, float soundVolume, int soundDensity) {
		this(plugin, launcher, radius, duration, cooldown, canMoveWhileCasting, true, chargeSound, soundVolume, soundDensity);
	}

	public SpellBaseAoE(Plugin plugin, LivingEntity launcher, int radius, int duration, int cooldown, boolean canMoveWhileCasting, boolean needLineOfSight,
	                    Sound chargeSound, float soundVolume, int soundDensity) {
		mPlugin = plugin;
		mLauncher = launcher;
		mRadius = radius;
		mDuration = duration;
		mCooldown = cooldown;
		mCanMoveWhileCasting = canMoveWhileCasting;
		mChargeSound = chargeSound;
		mSoundVolume = soundVolume;
		mSoundDensity = soundDensity;
		mLineOfSight = needLineOfSight;
	}

	protected abstract void chargeAuraAction(Location loc);

	protected abstract void chargeCircleAction(Location loc);

	protected abstract void outburstAction(Location loc);

	protected abstract void circleOutburstAction(Location loc);

	protected abstract void dealDamageAction(Location loc);

	@Override
	public void run() {
		if (mLineOfSight) {
			// Don't cast if no player in sight, e.g. should not initiate cast through a wall
			boolean hasLineOfSight = false;
			for (Player player : PlayerUtils.playersInRange(mLauncher.getLocation(), mRadius * 4, true)) {
				if (LocationUtils.hasLineOfSight(mLauncher, player)) {
					hasLineOfSight = true;
					break;
				}
			}
			if (!hasLineOfSight) {
				return;
			}
		}

		if (!mCanMoveWhileCasting) {
			mLauncher.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, mDuration, 20));
		}

		new BukkitRunnable() {
			float mTicks = 0;
			double mCurrentRadius = mRadius;
			final World mWorld = mLauncher.getWorld();

			@Override
			public void run() {
				Location loc = mLauncher.getLocation();

				if (mLauncher.isDead() || !mLauncher.isValid() || EntityUtils.isStunned(mLauncher) || EntityUtils.isSilenced(mLauncher)) {
					mLauncher.setAI(true);
					this.cancel();
					return;
				}
				mTicks++;
				chargeAuraAction(loc.clone().add(0, 1, 0));
				if (mTicks <= (mDuration - 5) && mTicks % mSoundDensity == 0) {
					mWorld.playSound(mLauncher.getLocation(), mChargeSound, SoundCategory.HOSTILE, mSoundVolume, 0.25f + (mTicks / 100));
				}
				for (double i = 0; i < 360; i += 30) {
					double radian1 = Math.toRadians(i);
					loc.add(FastUtils.cos(radian1) * mCurrentRadius, 0, FastUtils.sin(radian1) * mCurrentRadius);
					chargeCircleAction(loc);
					loc.subtract(FastUtils.cos(radian1) * mCurrentRadius, 0, FastUtils.sin(radian1) * mCurrentRadius);
				}
				mCurrentRadius -= (mRadius / ((double) mDuration));
				if (mCurrentRadius <= 0) {
					this.cancel();
					dealDamageAction(loc);
					outburstAction(loc);

					new BukkitRunnable() {
						final Location mLoc = mLauncher.getLocation();
						double mBurstRadius = 0;

						@Override
						public void run() {
							for (int j = 0; j < 2; j++) {
								mBurstRadius += 1.5;
								for (double i = 0; i < 360; i += 15) {
									double radian1 = Math.toRadians(i);
									mLoc.add(FastUtils.cos(radian1) * mBurstRadius, 0, FastUtils.sin(radian1) * mBurstRadius);
									circleOutburstAction(mLoc);
									mLoc.subtract(FastUtils.cos(radian1) * mBurstRadius, 0, FastUtils.sin(radian1) * mBurstRadius);
								}
							}
							if (mBurstRadius >= mRadius) {
								this.cancel();
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}

}

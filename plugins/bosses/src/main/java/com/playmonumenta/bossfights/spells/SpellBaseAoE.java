package com.playmonumenta.bossfights.spells;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/*
 * About damn time someone fixed this crappy ability.
 */
public class SpellBaseAoE extends Spell {

	@FunctionalInterface
	public interface ChargeAuraAction {
		/**
		 * Runs the large particle aura around the entity
		 * @param player The player to affect
		 */
		void run(Location loc);
	}

	@FunctionalInterface
	public interface ChargeCircleAction {
		/**
		 * Runs the large particle aura around the entity
		 * @param player The player to affect
		 */
		void run(Location loc);
	}

	@FunctionalInterface
	public interface OutburstAction {
		/**
		 * Runs the large particle aura around the entity
		 * @param player The player to affect
		 */
		void run(Location loc);
	}

	@FunctionalInterface
	public interface CircleOutburstAction {
		/**
		 * Runs the large particle aura around the entity
		 * @param player The player to affect
		 */
		void run(Location loc);
	}

	@FunctionalInterface
	public interface DealDamageAction {
		/**
		 * Runs the large particle aura around the entity
		 * @param player The player to affect
		 */
		void run(Location loc);
	}

	private final Plugin mPlugin;
	private final Entity mLauncher;
	private final int mRadius;
	private final int mDuration;
	private final int mCooldown;
	private final boolean mCanMoveWhileCasting;
	private final Sound mChargeSound;
	private final ChargeAuraAction mChargeAuraAction;
	private final ChargeCircleAction mChargeCircleAction;
	private final OutburstAction mOutburstAction;
	private final CircleOutburstAction mCircleOutburstAction;
	private final DealDamageAction mDealDamageAction;

	public SpellBaseAoE(Plugin plugin, Entity launcher, int radius, int duration, int cooldown, boolean canMoveWhileCasting,
	                    Sound chargeSound, ChargeAuraAction chargeAuraAction, ChargeCircleAction chargeCircleAction,
	                    OutburstAction outburstAction, CircleOutburstAction circleOutburstAction, DealDamageAction dealDamageAction) {
		mPlugin = plugin;
		mLauncher = launcher;
		mRadius = radius;
		mDuration = duration;
		mCooldown = cooldown;
		mCanMoveWhileCasting = canMoveWhileCasting;
		mChargeSound = chargeSound;
		mChargeAuraAction = chargeAuraAction;
		mChargeCircleAction = chargeCircleAction;
		mOutburstAction = outburstAction;
		mCircleOutburstAction = circleOutburstAction;
		mDealDamageAction = dealDamageAction;
	}

	@Override
	public void run() {

		World world = mLauncher.getWorld();
		if (!mCanMoveWhileCasting) {
			((LivingEntity) mLauncher).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, mDuration, 20));
		}
		new BukkitRunnable() {
			float j = 0;
			double radius = mRadius;

			@Override
			public void run() {
				Location loc = mLauncher.getLocation();

				if (mLauncher.isDead() || !mLauncher.isValid()) {
					this.cancel();
					return;
				}
				j++;
				mChargeAuraAction.run(loc.clone().add(0, 1, 0));
				if (j <= (mDuration - 5)) {
					world.playSound(mLauncher.getLocation(), mChargeSound, 1.5f, 0.25f + (j / 100));
				}
				for (double i = 0; i < 360; i += 12) {
					double radian1 = Math.toRadians(i);
					loc.add(Math.cos(radian1) * radius, 0, Math.sin(radian1) * radius);
					mChargeCircleAction.run(loc);
					loc.subtract(Math.cos(radian1) * radius, 0, Math.sin(radian1) * radius);
				}
				radius -= (mRadius / ((double) mDuration));
				if (radius <= 0) {
					this.cancel();
					mDealDamageAction.run(loc);
					mOutburstAction.run(loc);

					new BukkitRunnable() {
						Location loc = mLauncher.getLocation();
						double radius = 0;
						@Override
						public void run() {
							for (int j = 0; j < 2; j++) {
								radius += 1.5;
								for (double i = 0; i < 360; i += 15) {
									double radian1 = Math.toRadians(i);
									loc.add(Math.cos(radian1) * radius, 0, Math.sin(radian1) * radius);
									mCircleOutburstAction.run(loc);
									loc.subtract(Math.cos(radian1) * radius, 0, Math.sin(radian1) * radius);
								}
							}
							if (radius >= mRadius) {
								this.cancel();
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

	}

	@Override
	public int duration() {
		return 160 + mCooldown;
	}

}

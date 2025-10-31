package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public class SpellBarrier extends Spell {

	@FunctionalInterface
	public interface RefreshBarrierAction {
		/**
		 * The action that runs when the barrier comes up
		 *
		 * @param loc The location to run the effect
		 */
		void run(Location loc);
	}

	@FunctionalInterface
	public interface BarrierRunningAmbientAction {
		/**
		 * The action that runs every other tick to create a circle of particles while the barrier is active
		 *
		 * @param loc The location to run the effect
		 */
		void run(Location loc);
	}

	@FunctionalInterface
	public interface BreakBarrierAction {
		/**
		 * The action to run when the barrier gets broken
		 *
		 * @param loc The location at which to run the effect
		 */
		void run(Location loc);
	}

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mActivationRadius;
	private final int mRechargeTime;
	private final int mHitsToBreak;
	private final boolean mIsCarapace;
	private final RefreshBarrierAction mRefreshAction;
	private final BarrierRunningAmbientAction mRunningAmbientAction;
	private final BreakBarrierAction mBreakAction;
	private final double mCarapaceDamageModifier;

	private int mCurrentHits = 0;
	private boolean mActive = false;
	private int mTimer = 0;

	public SpellBarrier(final Plugin plugin, final LivingEntity boss, final int detectionRadius, final int rechargeTime,
	                    final int hitsToBreak, final boolean isCarapace, final RefreshBarrierAction refreshAction,
	                    final BarrierRunningAmbientAction ambientRunningAction, final BreakBarrierAction breakAction) {
		this(plugin, boss, detectionRadius, rechargeTime, hitsToBreak, isCarapace, refreshAction, ambientRunningAction, breakAction, 1.3);
	}

	public SpellBarrier(final Plugin plugin, final LivingEntity boss, final int detectionRadius, final int rechargeTime,
	                    final int hitsToBreak, final boolean isCarapace, final RefreshBarrierAction refreshAction,
	                    final BarrierRunningAmbientAction ambientRunningAction, final BreakBarrierAction breakAction, final double carapaceDamageModifier) {
		mPlugin = plugin;
		mBoss = boss;
		mActivationRadius = detectionRadius;
		mRechargeTime = rechargeTime;
		mHitsToBreak = hitsToBreak;
		mIsCarapace = isCarapace;
		mRefreshAction = refreshAction;
		mRunningAmbientAction = ambientRunningAction;
		mBreakAction = breakAction;
		mCarapaceDamageModifier = carapaceDamageModifier;
	}

	@Override
	public void run() {
		// Might as well not activate it outside of line of sight
		boolean hasLineOfSight = false;
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), mActivationRadius * 4, true)) {
			if (mBoss.hasLineOfSight(player)) {
				hasLineOfSight = true;
				break;
			}
		}
		if (!hasLineOfSight) {
			return;
		}

		mTimer -= BossAbilityGroup.PASSIVE_RUN_INTERVAL_DEFAULT;
		if (!mActive && mTimer <= 0) {
			mTimer = mRechargeTime;
			mActive = true;
			mRefreshAction.run(mBoss.getLocation());
			BukkitRunnable runnable = new BukkitRunnable() {
				@Override
				public void run() {
					if (mBoss.isDead() || !mBoss.isValid() || !mActive) {
						this.cancel();
					}
					mBoss.removePotionEffect(PotionEffectType.POISON);
					mBoss.removePotionEffect(PotionEffectType.WITHER);
					final Location location = mBoss.getLocation();
					for (double i = 0; i < 360; i += 15) {
						double radian1 = Math.toRadians(i);
						location.add(FastUtils.cos(radian1), 0, FastUtils.sin(radian1));
						mRunningAmbientAction.run(location);
						location.subtract(FastUtils.cos(radian1), 0, FastUtils.sin(radian1));
					}
				}
			};
			runnable.runTaskTimer(mPlugin, 0, TICKS_PER_SECOND);
			mActiveRunnables.add(runnable);
		}
	}

	@Override
	public void onHurt(final DamageEvent event) {
		if (mActive) {
			event.setCancelled(true);
			mCurrentHits++;
			if (mCurrentHits >= mHitsToBreak) {
				mCurrentHits = 0;
				mActive = false;
				mTimer = mRechargeTime;
				mBreakAction.run(mBoss.getLocation());
				return;
			}
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 1, 1);
		}
	}

	@Override
	public void onDamage(final DamageEvent event, final LivingEntity damagee) {
		if (mActive && mIsCarapace) {
			event.setFlatDamage(event.getFlatDamage() * mCarapaceDamageModifier);
		}
	}

	@Override
	public int castTicks() {
		return mRechargeTime;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}

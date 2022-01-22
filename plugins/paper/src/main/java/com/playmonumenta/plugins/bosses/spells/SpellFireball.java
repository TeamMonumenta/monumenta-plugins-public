package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.List;

public class SpellFireball extends Spell {
	@FunctionalInterface
	public interface LaunchFireballEffect {
		/**
		 * Runs at location of boss and target when generating a fireball
		 * @param loc The location of boss and player
		 */
		void run(Location loc);
	}

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mRange;
	private final int mDelay;
	private final int mCount;
	private final float mYield;
	private final boolean mIsIncendiary;
	private final boolean mSingleTarget;
	private final LaunchFireballEffect mLaunchEffect;
	private final int mDuration;

	/**
	 * @param plugin          Plugin
	 * @param boss            Boss
	 * @param range           Range within which players may be targeted
	 * @param delay           Delay between fireballs
	 * @param count           Number of fireballs spawned (per attack)
	 * @param yield           Explosive power
	 * @param isIncendiary    Creates fire or not
	 * @param singleTarget    Target random player (true) or all players (false)
	 * @param launchEffect    Function to run on boss and targeted player(s)
	 */
	public SpellFireball(Plugin plugin, LivingEntity boss, int range, int delay, int count, int duration,
	                     float yield, boolean isIncendiary, boolean singleTarget,
	                     LaunchFireballEffect launchEffect) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mDelay = delay;
		mCount = count;
		mDuration = duration;
		mYield = yield;
		mIsIncendiary = isIncendiary;
		mSingleTarget = singleTarget;
		mLaunchEffect = launchEffect;
	}

	@Override
	public void run() {
		BukkitRunnable runnable = new BukkitRunnable() {
			private int mTicks = 0;
			private int mLaunches = 0;
			private List<Player> mPlayers = PlayerUtils.playersInRange(mBoss.getLocation(), mRange, true);

			@Override
			public void run() {
				mBoss.setAI(false);
				mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 40, 2, false, false));
				mBoss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false));
				for (Player player : mPlayers) {
					player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 2, 2f);
				}
				if (mTicks >= mDelay) {
					mLaunches++;
					mTicks = 0;

					mPlayers = PlayerUtils.playersInRange(mBoss.getLocation(), mRange, false);
					if (mSingleTarget) {
						// Single target chooses a random player within range that has line of sight
						Collections.shuffle(mPlayers);
						for (Player player : mPlayers) {
							if (!player.getGameMode().equals(GameMode.CREATIVE) && LocationUtils.hasLineOfSight(mBoss, player)) {
								launch(player);
								break;
							}
						}
					} else {
						// Otherwise target all players within range
						for (Player player : mPlayers) {
							if (!player.getGameMode().equals(GameMode.CREATIVE)) {
								launch(player);
							}
						}
					}
				}

				if (mLaunches >= mCount) {
					this.cancel();
					mBoss.setAI(true);
					mActiveRunnables.remove(this);
				}

				mTicks += 2;
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return mDuration;
	}

	private void launch(Player target) {
		// Play launch effect
		mLaunchEffect.run(target.getLocation());
		mLaunchEffect.run(mBoss.getLocation());

		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				// Summon a fireball slightly offset from the boss in the direction of the player
				Location spawnLoc = mBoss.getEyeLocation();
				Vector direction = target.getEyeLocation().subtract(spawnLoc).toVector().normalize();
				spawnLoc = spawnLoc.add(direction.multiply(2));
				Fireball fireball = (Fireball)mBoss.getWorld().spawnEntity(spawnLoc, EntityType.FIREBALL);
				fireball.setDirection(direction);
				fireball.setVelocity(direction.multiply(2));
				fireball.setYield(mYield);
				fireball.setIsIncendiary(mIsIncendiary);
				mActiveRunnables.remove(this);
			}
		};

		runnable.runTaskLater(mPlugin, 14);
		mActiveRunnables.add(runnable);
	}
}

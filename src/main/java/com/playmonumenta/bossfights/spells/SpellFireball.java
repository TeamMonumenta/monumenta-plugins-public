package com.playmonumenta.bossfights.spells;

import com.playmonumenta.bossfights.utils.Utils;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

public class SpellFireball implements Spell {
	@FunctionalInterface
	public interface LaunchFireballEffect {
		/**
		 * Runs at location of boss and target when generating a fireball
		 * @param loc The location of boss and player
		 */
		void run(Location loc);
	}

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private int mRange;
	private int mDelay;
	private int mCount;
	private float mYield;
	private boolean mIsIncendiary;
	private boolean mSingleTarget;
	private LaunchFireballEffect mLaunchEffect;

	private Random mRandom = new Random();

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
	public SpellFireball(Plugin plugin, LivingEntity boss, int range, int delay, int count,
	                     float yield, boolean isIncendiary, boolean singleTarget,
	                     LaunchFireballEffect launchEffect) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mDelay = delay;
		mCount = count;
		mYield = yield;
		mIsIncendiary = isIncendiary;
		mSingleTarget = singleTarget;
		mLaunchEffect = launchEffect;
	}

	@Override
	public void run() {
		new BukkitRunnable() {
			private int mTicks = 0;
			private int mLaunches = 0;
			private List<Player> players = Utils.playersInRange(mBoss.getLocation(), mRange);

			@Override
			public void run() {
				mBoss.setAI(false);
				mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 40, 2, false, false));
				mBoss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false));
				for (Player player : players) {
					player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 2, 2f);
				}
				if (mTicks >= mDelay) {
					mLaunches++;
					mTicks = 0;

					players = Utils.playersInRange(mBoss.getLocation(), mRange);
					if (mSingleTarget) {
						// Single target chooses a random player within range that has line of sight
						Collections.shuffle(players);
						for (Player player : players) {
							if (Utils.hasLineOfSight(player, mBoss)) {
								launch(player);
								break;
							}
						}
					} else {
						// Otherwise target all players within range
						for (Player player : players) {
							launch(player);
						}
					}
				}

				if (mLaunches >= mCount) {
					this.cancel();
					mBoss.setAI(true);
				}

				mTicks += 2;
			}
		}.runTaskTimer(mPlugin, 0, 2);
	}

	@Override
	public int duration() {
		return 160; // 8 seconds
	}

	private void launch(Player target) {
		// Play launch effect
		mLaunchEffect.run(target.getLocation());
		mLaunchEffect.run(mBoss.getLocation());

		new BukkitRunnable() {
			@Override
			public void run() {
				// Summon a fireball slightly offset from the boss in the direction of the player
				Location spawnLoc = mBoss.getLocation().add(0, 0.5, 0);
				Vector direction = target.getLocation().subtract(spawnLoc).toVector().normalize();
				spawnLoc = spawnLoc.add(direction);
				Fireball fireball = (Fireball)mBoss.getWorld().spawnEntity(spawnLoc, EntityType.FIREBALL);
				fireball.setDirection(direction);
				fireball.setYield(mYield);
				fireball.setIsIncendiary(mIsIncendiary);
			}
		}.runTaskLater(mPlugin, 14);
	}
}

package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellSteelboreSpread extends Spell {

	private final String ABILITY_NAME = "Steelbore Spread";
	private final int CAST_TIME = 20 * 7;
	private int mRadius;
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private ChargeUpManager mChargeUp;
	private Location mStartLoc;
	private int mRange;
	private double mDamage;

	public SpellSteelboreSpread(Plugin plugin, LivingEntity boss, int radius, Location startLoc, int range, double damage) {
		mPlugin = plugin;
		mBoss = boss;
		mRadius = radius;
		mStartLoc = startLoc;
		mRange = range;
		mDamage = damage;
		mChargeUp = new ChargeUpManager(mBoss, CAST_TIME, ChatColor.GOLD + "Casting " + ChatColor.YELLOW + ABILITY_NAME,
			BarColor.YELLOW, BarStyle.SOLID, mRange);
	}

	@Override
	public void run() {
		List<Player> plays = PlayerUtils.playersInRange(mStartLoc, mRange, true);

		for (Player p : plays) {
			mBoss.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 1, 2);
		}
		mChargeUp.setTime(0);
		BukkitRunnable testrunnable = new BukkitRunnable() {
			@Override
			public void run() {
				List<Player> players = PlayerUtils.playersInRange(mStartLoc, mRange, true);

				if (mChargeUp.nextTick(2)) {
					int maxSelectionHeight = 30;
					for (Player p : players) {
						List<Player> nearby = new ArrayList<>();
						// Used for getting all players above/below
						for (int i = 0; i < maxSelectionHeight; i += mRadius) {
							for (Player tempP : PlayerUtils.playersInRange(p.getLocation().add(0, i, 0), mRadius, true)) {
								if (!nearby.contains(tempP)) {
									nearby.add(tempP);
								}
							}
							for (Player tempP : PlayerUtils.playersInRange(p.getLocation().add(0, -i, 0), mRadius, true)) {
								if (!nearby.contains(tempP)) {
									nearby.add(tempP);
								}
							}
						}
						for (Player n : nearby) {
							new BukkitRunnable() {
								int mT = 0;
								int ANIM_TIME = 10;
								int MAX_HEIGHT = 5;
								@Override
								public void run() {
									if (mT >= ANIM_TIME) {
										if (!p.equals(n)) {
											BossUtils.bossDamagePercent(mBoss, n, mDamage, ABILITY_NAME);
										}
										n.playSound(n.getLocation(), Sound.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, 1, 0);
										this.cancel();
									}
									new PPCircle(Particle.REDSTONE, p.getLocation().add(0, MAX_HEIGHT - (mT / 2f), 0), mRadius * 0.5).ringMode(true).count(25).delta(0.1, 0.05, 0.1).data(new Particle.DustOptions(Color.fromRGB(195, 37, 37), 1.65f)).spawnAsBoss();
									new PPCircle(Particle.SQUID_INK, p.getLocation().add(0, MAX_HEIGHT - (mT / 2f), 0), mRadius * 0.5).ringMode(true).count(25).delta(0.1, 0.05, 0.1).spawnAsBoss();
									PPCircle indicator2 = new PPCircle(Particle.REDSTONE, p.getLocation(), 0).ringMode(true).count(2).delta(0.25, 0.1, 0.25).data(new Particle.DustOptions(Color.fromRGB(255, 63, 63), 1.65f));
									for (double r = 1; r < mRadius; r++) {
										indicator2.radius(r).location(p.getLocation()).spawnAsBoss();
									}
									mT += 1;
								}
							}.runTaskTimer(mPlugin, 0, 1);
						}
					}
					this.cancel();
				} else {
					for (Player p : players) {
						boolean notifyPlayer = false;
						if (mChargeUp.getTime() % 20 == 0) {
							for (Player otherP : players) {
								if (p != otherP && p.getLocation().distance(otherP.getLocation()) <= mRadius) {
									notifyPlayer = true;
								}
							}
							if (mChargeUp.getTime() % 40 == 0) {
								if (notifyPlayer) {
									p.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, p.getLocation(), 6, 0.35, 0, 0.35, 0.05);
								}
							}
						}
						p.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, p.getLocation().clone().add(0, 2, 0), 1, 0.35, 0, 0.35, 0.05);
						/*
						PPCircle indicator2 = new PPCircle(Particle.REDSTONE, p.getLocation(), 0).ringMode(true).count(2).delta(0.25, 0.1, 0.25).data(new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.65f));
						for (double r = 1; r < AOE_RADIUS; r++) {
							indicator2.radius(r).location(p.getLocation()).spawnAsBoss();
						}
						*/
						if (notifyPlayer) {
							new PPCircle(Particle.REDSTONE, p.getLocation().add(0, 0.25, 0), mRadius).ringMode(true).count(30).delta(0.1, 0.05, 0.1).data(new Particle.DustOptions(Color.fromRGB(225, 55, 55), 1.65f)).spawnAsBoss();
							new PPCircle(Particle.FLAME, p.getLocation().add(0, 0.25, 0), mRadius).ringMode(true).count(10).delta(0.1, 0.05, 0.1).spawnAsBoss();
						} else {
							new PPCircle(Particle.REDSTONE, p.getLocation().add(0, 0.25, 0), mRadius).ringMode(true).count(30).delta(0.1, 0.05, 0.1).data(new Particle.DustOptions(Color.fromRGB(252, 3, 3), 1.65f)).spawnAsBoss();
							new PPCircle(Particle.ELECTRIC_SPARK, p.getLocation().add(0, 0.25, 0), mRadius).ringMode(true).count(10).delta(0.1, 0.05, 0.1).spawnAsBoss();
						}
					}
				}
			}
		};
		testrunnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(testrunnable);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 8;
	}
}

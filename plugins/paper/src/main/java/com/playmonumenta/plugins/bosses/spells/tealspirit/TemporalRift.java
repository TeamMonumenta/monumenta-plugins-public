package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.TealSpirit;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class TemporalRift extends Spell {
	private static final int CHARGE_TIME = 4 * 20;
	private static final int DURATION = 5 * 20;
	private static final double DAMAGE_OUTER = 30;
	private static final double DAMAGE_INNER = 60;
	private static final double RADIUS = 6;
	private static final double RADIUS_INNER = 3;

	private static final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(185, 0, 0), 1.0f);
	private static final Particle.DustOptions YELLOW = new Particle.DustOptions(Color.fromRGB(185, 185, 0), 1.0f);

	private final LivingEntity mBoss;
	private final Location mCenter;
	private final int mCooldownTicks;

	private double mRad = 10;
	private double mRotation = 0;
	private final ChargeUpManager mChargeUp;
	private final PartialParticle mPortal2;
	private final PartialParticle mSmokeN;
	private final PartialParticle mWitch;
	private final PartialParticle mSmokeL;
	private final PartialParticle mBreath;

	public TemporalRift(LivingEntity boss, Location center, int cooldownTicks) {
		mBoss = boss;
		mCenter = center;
		mCooldownTicks = cooldownTicks;

		mChargeUp = new ChargeUpManager(mBoss, CHARGE_TIME, ChatColor.YELLOW + "Channeling Temporal Rift...", BarColor.YELLOW, BarStyle.SOLID, 70);
		mPortal2 = new PartialParticle(Particle.PORTAL, mBoss.getLocation(), 2, 0.15, 0.15, 0.15, 0.1);
		mSmokeN = new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation(), 1, 0.15, 0.15, 0.15, 0);
		mWitch = new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation(), 1, 0.15, 0.15, 0.15, 0);
		mSmokeL = new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 30, 0, 0, 0, 0.25);
		mBreath = new PartialParticle(Particle.DRAGON_BREATH, mBoss.getLocation(), 100, 0, 0, 0, 0.25);
	}

	@Override
	public void run() {

		Plugin plugin = Plugin.getInstance();
		World world = mBoss.getWorld();

		PPCircle indicator = new PPCircle(Particle.REDSTONE, mBoss.getLocation(), RADIUS).ringMode(true).count(25).delta(0.01).data(YELLOW);
		PPCircle indicator2 = new PPCircle(Particle.REDSTONE, mBoss.getLocation(), RADIUS).ringMode(true).count(25).delta(0.01).data(RED);

		List<Location> locs = new ArrayList<>();
		for (Player player : PlayerUtils.playersInRange(mCenter, TealSpirit.detectionRange, true)) {
			locs.add(player.getLocation());
		}

		for (Location loc : locs) {
			world.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 0.6f, 0.5f);
			world.playSound(loc, Sound.BLOCK_PORTAL_TRAVEL, 0.6f, 1.0f);
			loc.add(0, 0.2, 0);
		}

		BukkitRunnable runA = new BukkitRunnable() {
			@Override
			public void run() {
				mRotation += 7.5;
				mRad -= 0.5;
				for (Location loc : locs) {
					indicator.radius(RADIUS).location(loc).spawnAsBoss();
					for (int i = 0; i < 6; i++) {
						double radian = Math.toRadians(mRotation + (60 * i));
						loc.add(FastUtils.cos(radian) * mRad, 0, FastUtils.sin(radian) * mRad);
						mWitch.location(loc).spawnAsBoss();
						mPortal2.location(loc).spawnAsBoss();
						mSmokeN.location(loc).spawnAsBoss();
						loc.subtract(FastUtils.cos(radian) * mRad, 0, FastUtils.sin(radian) * mRad);
					}
				}

				if (mRad <= 0) {
					mRad = 10;
				}

				if (mChargeUp.nextTick()) {
					this.cancel();
					mChargeUp.setTitle(ChatColor.YELLOW + "Casting Temporal Rift...");
					mChargeUp.setColor(BarColor.RED);
					locs.forEach(loc -> world.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 2.0f));
					mSmokeL.location(mBoss.getLocation()).spawnAsBoss();
					mBreath.location(mBoss.getLocation()).spawnAsBoss();
					BukkitRunnable runB = new BukkitRunnable() {
						int mT = 0;

						@Override
						public void run() {
							mT++;
							mChargeUp.setProgress(1 - ((double) mT / DURATION));

							for (Location loc : locs) {
								List<Player> players = PlayerUtils.playersInRange(loc, RADIUS, true);
								if (mT % 15 == 0 && mT < DURATION - 40) {
									world.playSound(loc, Sound.BLOCK_PORTAL_AMBIENT, 0.75f, 0.5f);
								}
								indicator2.radius(RADIUS).location(loc).spawnAsBoss();

								for (Player player : players) {
									if (mT % 15 == 0) {
										Location clone = loc.clone();
										Vector dir = LocationUtils.getDirectionTo(player.getLocation(), clone);

										if (player.getLocation().distance(loc) > RADIUS_INNER) {
											DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE_OUTER, null, false, true, "Temporal Rift");
										}
										player.setVelocity(dir.multiply(-0.65));
									}

									if (mT % 10 == 0) {
										if (player.getLocation().distance(loc) <= RADIUS_INNER) {
											DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE_INNER, null, false, true, "Temporal Rift");
										}
									}
								}
							}

							mRotation += 7.5;
							mRad -= RADIUS / 20;
							for (Location loc : locs) {
								for (int i = 0; i < 6; i++) {
									double radian = Math.toRadians(mRotation + (60 * i));
									loc.add(FastUtils.cos(radian) * mRad, 0, FastUtils.sin(radian) * mRad);
									mWitch.location(loc).spawnAsBoss();
									mPortal2.location(loc).spawnAsBoss();
									mSmokeN.location(loc).spawnAsBoss();
									loc.subtract(FastUtils.cos(radian) * mRad, 0, FastUtils.sin(radian) * mRad);
								}
							}

							if (mRad <= 0) {
								mRad = RADIUS;
							}
							if (mT >= DURATION) {
								this.cancel();
							}
						}

						@Override
						public synchronized void cancel() {
							super.cancel();
							mChargeUp.reset();
							mChargeUp.setColor(BarColor.YELLOW);
						}

					};
					runB.runTaskTimer(plugin, 0, 1);
					mActiveRunnables.add(runB);
				} else if (mChargeUp.getTime() < CHARGE_TIME / 2) {
					locs.clear();
					PlayerUtils.playersInRange(mCenter, TealSpirit.detectionRange, true).forEach(player -> locs.add(player.getLocation()));
				}
			}

		};
		runA.runTaskTimer(plugin, 0, 1);
		mActiveRunnables.add(runA);
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}
}

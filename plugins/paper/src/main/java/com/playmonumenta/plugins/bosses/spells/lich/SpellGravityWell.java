package com.playmonumenta.plugins.bosses.spells.lich;

import java.util.Collections;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.player.PPGroundCircle;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;

/*
Gravity Well - A well of gravity is created at the boss, every 0.75 seconds players within line of
sight of the well and within 12 blocks are pulled towards it, if a player is within line of sight of the
well they take 20 damage every 0.75 seconds. Lasts 9 seconds.
if the player is within 4 blocks of the singularity they take 35 damage per 10 ticks -new
 */
public class SpellGravityWell extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mCenter;
	private double mRange;
	private boolean mCooldown = false;
	private int mRadius = 10;
	private double mRad = 10;
	private double mRotation = 0;
	private static final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(185, 0, 0), 1.0f);
	private static final Particle.DustOptions YELLOW = new Particle.DustOptions(Color.fromRGB(185, 185, 0), 1.0f);
	private ChargeUpManager mChargeUp;

	public SpellGravityWell(Plugin plugin, LivingEntity boss, Location center, double range) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
		mRange = range;
		mChargeUp = new ChargeUpManager(mBoss, 40, ChatColor.YELLOW + "Channeling Gravity Well...", BarColor.YELLOW, BarStyle.SOLID, 70);
	}

	@Override
	public void run() {
		mCooldown = true;
		new BukkitRunnable() {

			@Override
			public void run() {
				mCooldown = false;
			}

		}.runTaskLater(mPlugin, 20 * 30);
		World world = mBoss.getWorld();
		new PartialParticle(Particle.PORTAL, mBoss.getLocation(), 100, 0.1, 0.1, 0.1, 0.1).spawnAsBoss();

		PPGroundCircle indicator = new PPGroundCircle(Particle.REDSTONE, mBoss.getLocation(), 75, 0.01, 0.01, 0.01, 0, YELLOW).init(mRadius, true);
		PPGroundCircle indicator2 = new PPGroundCircle(Particle.REDSTONE, mBoss.getLocation(), 75, 0.01, 0.01, 0.01, 0, RED).init(mRadius, true);

		List<Player> players = Lich.playersInRange(mCenter, mRange, true);
		players.removeIf(p -> SpellDimensionDoor.getShadowed().contains(p));
		Collections.shuffle(players);
		Player p = players.get(0);
		world.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 3.0f, 0.5f);
		world.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 3.0f, 1.0f);

		BukkitRunnable runA = new BukkitRunnable() {
			Location mLoc = p.getLocation().add(0, 0.2, 0);
			@Override
			public void run() {

				mRotation += 7.5;
				mRad -= 0.5;
				indicator.radius(mRadius).location(mLoc).spawnAsBoss();
				for (int i = 0; i < 6; i++) {
					double radian = Math.toRadians(mRotation + (60*i));
					mLoc.add(FastUtils.cos(radian) * mRad, 0, FastUtils.sin(radian) * mRad);
					new PartialParticle(Particle.SPELL_WITCH, mLoc, 2, 0.15, 0.15, 0.15, 0).spawnAsBoss();
					new PartialParticle(Particle.PORTAL, mLoc, 2, 0.15, 0.15, 0.15, 0.1).spawnAsBoss();
					new PartialParticle(Particle.SMOKE_NORMAL, mLoc, 1, 0.15, 0.15, 0.15, 0).spawnAsBoss();
					mLoc.subtract(FastUtils.cos(radian) * mRad, 0, FastUtils.sin(radian) * mRad);
				}
				if (mRad <= 0) {
					mRad = 10;
				}

				if (mChargeUp.nextTick() || Lich.phase3over()) {
					this.cancel();
					mChargeUp.setTitle(ChatColor.YELLOW + "Casting Gravity Well...");
					mChargeUp.setColor(BarColor.RED);
					world.playSound(mLoc, Sound.BLOCK_END_PORTAL_SPAWN, 3.0f, 2.0f);
					new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 50, 0, 0, 0, 0.25).spawnAsBoss();
					new PartialParticle(Particle.DRAGON_BREATH, mBoss.getLocation(), 160, 0, 0, 0, 0.25).spawnAsBoss();
					BukkitRunnable runB = new BukkitRunnable() {
						int mT = 0;
						@Override
						public void run() {
							mT++;
							mChargeUp.setProgress(1.0d - (mT / (20 * 9.0d)));
							List<Player> players = Lich.playersInRange(mLoc, mRadius, true);
							if (mT % 15 == 0) {
								world.playSound(mLoc, Sound.BLOCK_PORTAL_AMBIENT, 3.0f, 0.5f);
							}
							indicator2.radius(mRadius).location(mLoc).spawnAsBoss();

							for (Player player : players) {
								if (mT % 15 == 0) {
									Location clone = mLoc.clone();
									Vector dir = LocationUtils.getDirectionTo(player.getLocation(), clone);
									boolean pull = false;
									for (int i = 0; i < 200; i++) {
										clone.add(dir.clone().multiply(0.25));

										if (clone.getBlock().getType().isSolid()) {
											break;
										}

										if (clone.distance(player.getLocation()) < 0.75) {
											pull = true;
											break;
										}
									}

									if (pull) {
										if (player.getLocation().distance(mLoc) > 4) {
											BossUtils.bossDamage(mBoss, player, 20, null, "Gravity Well");
										}
										player.setVelocity(dir.multiply(-0.85));
									}
								}

								if (mT % 10 == 0) {
									if (player.getLocation().distance(mLoc) <= 4) {
										BossUtils.bossDamage(mBoss, player, 35, null, "Gravity Well");
									}
								}
							}

							mRotation += 7.5;
							mRad -= 0.5;
							for (int i = 0; i < 6; i++) {
								double radian = Math.toRadians(mRotation + (60*i));
								mLoc.add(FastUtils.cos(radian) * mRad, 0, FastUtils.sin(radian) * mRad);
								new PartialParticle(Particle.SPELL_WITCH, mLoc, 2, 0.15, 0.15, 0.15, 0).spawnAsBoss();
								new PartialParticle(Particle.PORTAL, mLoc, 2, 0.15, 0.15, 0.15, 0.1).spawnAsBoss();
								new PartialParticle(Particle.SMOKE_NORMAL, mLoc, 1, 0.15, 0.15, 0.15, 0).spawnAsBoss();
								mLoc.subtract(FastUtils.cos(radian) * mRad, 0, FastUtils.sin(radian) * mRad);
							}
							if (mRadius <= 0) {
								mRadius = 10;
							}
							if (mRad <= 0) {
								mRad = 10;
							}
							if (mT >= 20 * 9 || Lich.phase3over()) {
								this.cancel();
								mChargeUp.reset();
								mChargeUp.setColor(BarColor.YELLOW);
							}
						}

					};
					runB.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(runB);
				}
			}

		};
		runA.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runA);
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}

	@Override
	public int cooldownTicks() {
		return 20 * 15;
	}

}

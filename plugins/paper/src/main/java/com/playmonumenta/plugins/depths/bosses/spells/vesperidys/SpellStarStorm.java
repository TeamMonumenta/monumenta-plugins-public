package com.playmonumenta.plugins.depths.bosses.spells.vesperidys;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellStarStorm extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Vesperidys mVesperidys;

	private static final Material BULLET_MATERIAL = Material.AMETHYST_BLOCK;
	private static final double HITBOX = 0.3125;
	private static final double DIRECT_HIT_DAMAGE = 60;
	private static final String SPELL_NAME = "Star Storm";

	private final PartialParticle mPHit;
	private final List<Player> mHitPlayers = new ArrayList<>();
	private final ChargeUpManager mChargeUp;

	private static final int PATTERN1_CHARGE_TIME = 70;
	private static final int PATTERN1_BULLET_DURATION = 10 * 20;
	private static final int PATTERN1_NUM_BULLETS = 60;

	private static final int PATTERN2_CHARGE_TIME = 50;
	private static final int PATTERN2_BULLET_DURATION = 50 * 4;
	private static final int PATTERN2_BARRAGES = 1;
	private static final int PATTERN2_BARRAGES_ASCENSION = 2;

	private boolean mOnCooldown = false;

	public SpellStarStorm(Plugin plugin, LivingEntity boss, Vesperidys vesperidys) {
		mPlugin = plugin;
		mBoss = boss;
		mVesperidys = vesperidys;

		mChargeUp = new ChargeUpManager(mBoss, PATTERN1_CHARGE_TIME, Component.text("Charging ", NamedTextColor.GREEN).append(Component.text("Star Storm...", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)),
			BossBar.Color.PINK, BossBar.Overlay.NOTCHED_10, 100);
		mPHit = new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 20, 0.25, 0.25, 0.25, 0.25);

	}

	@Override
	public void run() {
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mOnCooldown = false;
		}, cooldownTicks() + 20);

		if (FastUtils.randomIntInRange(0, 1) == 0) {
			pattern1();
		} else {
			pattern2();
		}
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown && !mVesperidys.mTeleportSpell.mTeleporting;
	}

	@Override
	public int cooldownTicks() {
		return mVesperidys.mSpellCooldowns;
	}

	private void pattern1() {
		mChargeUp.setChargeTime(PATTERN1_CHARGE_TIME);
		mVesperidys.mTeleportSpell.teleportPlatform(0, 0);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 5, 1);
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 5, 1.4f);
			mHitPlayers.clear();

			BukkitRunnable runnableA = new BukkitRunnable() {

				@Override
				public void run() {
					if (mChargeUp.nextTick(2)) {
						this.cancel();

						mChargeUp.setTitle(Component.text("Unleashing ", NamedTextColor.GREEN).append(Component.text("Star Storm...", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)));
						BukkitRunnable runnableB = new BukkitRunnable() {
							int mT = 0;

							@Override
							public synchronized void cancel() {
								super.cancel();
								mChargeUp.reset();
								mChargeUp.setTitle(Component.text("Charging ", NamedTextColor.GREEN).append(Component.text("Star Storm...", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)));
							}

							@Override
							public void run() {
								mChargeUp.setProgress(1 - ((double) mT / PATTERN1_BULLET_DURATION));
								if (mT > PATTERN1_BULLET_DURATION) {
									this.cancel();
								}
								mT++;
							}
						};
						runnableB.runTaskTimer(mPlugin, 0, 1);
						mActiveRunnables.add(runnableB);
					}
				}
			};
			runnableA.runTaskTimer(mPlugin, 0, 2);
			mActiveRunnables.add(runnableA);

			BukkitRunnable runnableC = new BukkitRunnable() {
				int mT = 0;
				int mBullets = 0;

				@Override
				public void run() {
					// Aim to spawn everything in 3 seconds, which suggests spawning 1 every tick.
					// If Phase 4, then spawn two bullets per tick.
					double r = 25;
					double angle = (180.0 / PATTERN1_NUM_BULLETS) * mBullets;
					double radians = Math.toRadians(angle);
					Location loc = mVesperidys.mSpawnLoc.clone().add(r * Math.cos(radians), 0, r * Math.sin(radians));
					Location loc2 = mVesperidys.mSpawnLoc.clone().add(-r * Math.cos(radians), 0, -r * Math.sin(radians));
					loc.setY(Math.floor(mVesperidys.mSpawnLoc.getY()) + 0.1875);
					loc2.setY(Math.floor(mVesperidys.mSpawnLoc.getY()) + 0.1875);

					int timeStart = PATTERN1_CHARGE_TIME - mT;
					launchPattern1Bullet(loc, timeStart);
					launchPattern1Bullet(loc2, timeStart);
					mBullets += 2;

					loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_STEP, SoundCategory.HOSTILE, 5, 1);
					loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.HOSTILE, 5, 0.5f + 1.5f * mT / PATTERN1_CHARGE_TIME);


					if (mBullets >= PATTERN1_NUM_BULLETS) {
						this.cancel();
					}

					mT += 2;
				}
			};
			runnableC.runTaskTimer(mPlugin, 0, 2);
			mActiveRunnables.add(runnableC);
		}, 20);
	}

	private void pattern2() {
		mChargeUp.setChargeTime(PATTERN2_CHARGE_TIME);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 5, 1);
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 5, 1.4f);
			mHitPlayers.clear();

			BukkitRunnable runnableA = new BukkitRunnable() {

				@Override
				public void run() {
					if (mChargeUp.nextTick(2)) {
						this.cancel();

						mChargeUp.setTitle(Component.text("Unleashing ", NamedTextColor.GREEN).append(Component.text("Star Storm...", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)));
						BukkitRunnable runnableB = new BukkitRunnable() {
							int mT = 0;

							@Override
							public synchronized void cancel() {
								super.cancel();
								mChargeUp.reset();
								mChargeUp.setTitle(Component.text("Charging ", NamedTextColor.GREEN).append(Component.text("Star Storm...", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)));
							}

							@Override
							public void run() {
								mChargeUp.setProgress(1 - ((double) mT / PATTERN2_BULLET_DURATION));
								if (mT > PATTERN2_BULLET_DURATION) {
									this.cancel();
								}
								mT++;
							}
						};
						runnableB.runTaskTimer(mPlugin, 0, 1);
						mActiveRunnables.add(runnableB);
					}
				}
			};
			runnableA.runTaskTimer(mPlugin, 0, 2);
			mActiveRunnables.add(runnableA);

			BukkitRunnable runnableD = new BukkitRunnable() {
				int mCount = 0;

				@Override
				public void run() {
					if (mCount > 3) {
						this.cancel();
						return;
					}
					mCount++;

					ArrayList<Integer> directions = new ArrayList<>(List.of(0, 1, 2, 3));
					Collections.shuffle(directions);
					int numBarrage = ((mVesperidys.mParty != null && mVesperidys.mParty.getAscension() >= 15) ? PATTERN2_BARRAGES_ASCENSION : PATTERN2_BARRAGES);

					for (int i = 0; i < numBarrage; i++) {
						int direction = directions.get(i);

						Vector dirProj;
						Vector dirLine;

						Location startLoc = mVesperidys.mSpawnLoc.clone();
						switch (direction) {
							case 0 -> {
								startLoc.add(-18, 0, -18);
								dirLine = new Vector(1, 0, 0);
								dirProj = new Vector(0, 0, 1);
							}
							case 1 -> {
								startLoc.add(18, 0, -18);
								dirLine = new Vector(0, 0, 1);
								dirProj = new Vector(-1, 0, 0);
							}
							case 2 -> {
								startLoc.add(18, 0, 18);
								dirLine = new Vector(-1, 0, 0);
								dirProj = new Vector(0, 0, -1);
							}
							default -> {
								startLoc.add(-18, 0, 18);
								dirLine = new Vector(0, 0, -1);
								dirProj = new Vector(1, 0, 0);
							}
						}

						BukkitRunnable runnableC = new BukkitRunnable() {
							int mT = 0;
							int mBullets = 0;
							final Location mLocation = startLoc;

							@Override
							public void run() {
								Location loc = mLocation.add(dirLine);
								loc.setY(Math.floor(mVesperidys.mSpawnLoc.getY()) + 0.1875);

								int timeStart = PATTERN2_CHARGE_TIME - mT;
								launchPattern2Bullet(loc, dirProj, timeStart);
								mBullets += 1;

								if (mT % 2 == 0) {
									mLocation.getWorld().playSound(mLocation, Sound.BLOCK_AMETHYST_BLOCK_STEP, SoundCategory.HOSTILE, 5, 1);
									mLocation.getWorld().playSound(mLocation, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.HOSTILE, 5, 0.5f + 1.5f * mT / PATTERN2_CHARGE_TIME);
								}

								if (mBullets >= 35) {
									this.cancel();
								}

								mT += 1;
							}
						};
						runnableC.runTaskTimer(mPlugin, 0, 1);
						mActiveRunnables.add(runnableC);
					}
				}
			};

			runnableD.runTaskTimer(mPlugin, 0, PATTERN2_CHARGE_TIME);
			mActiveRunnables.add(runnableD);
		}, 20);
	}

	private void launchPattern1Bullet(Location detLoc, double accelStart) {
		List<Player> players = PlayerUtils.playersInRange(detLoc, 75, true);

		BlockDisplay bullet = spawnBullet(detLoc, false);

		// Determine Theta and R.
		Location newLoc = detLoc.clone().subtract(mVesperidys.mSpawnLoc);
		double r = detLoc.distance(mVesperidys.mSpawnLoc);
		double theta = Math.atan2(newLoc.getZ(), newLoc.getX());

		BukkitRunnable runnableBullet = new BukkitRunnable() {
			final Location mLocation = detLoc;
			int mTicks = 0;
			double mR = r;
			double mTheta = theta;
			double MIN_R = 1;
			double mInnerVelocity = 0;

			@Override
			public void run() {
				// Iterate two times and half the velocity so that way we can have more accurate travel for intersection.
				Location oldLoc = mLocation.clone();

				for (int j = 0; j < 2; j++) {
					if (mTicks < PATTERN1_BULLET_DURATION * 2 / 3 && mR > MIN_R) {
						mR -= mInnerVelocity * 0.5;
					} else {
						mR += mInnerVelocity * 0.5;
					}
					mTheta += mInnerVelocity * 0.5 * Math.toRadians(5);

					mLocation.set(mVesperidys.mSpawnLoc.getX() + mR * Math.cos(mTheta), mLocation.getY(), mVesperidys.mSpawnLoc.getZ() + mR * Math.sin(mTheta));
					BoundingBox box = BoundingBox.of(mLocation, HITBOX, HITBOX, HITBOX);
					for (Player player : players) {
						if (player.getBoundingBox().overlaps(box)) {
							directHit(player);
							MovementUtils.knockAway(oldLoc, player, 1f, 0.5f);
							this.cancel();
							return;
						}
					}

					double dX = mLocation.getX() - mVesperidys.mSpawnLoc.getX();
					double dZ = mLocation.getZ() - mVesperidys.mSpawnLoc.getZ();
					if (mLocation.getBlock().getType() != Material.AIR && !BlockUtils.isMechanicalBlock(mLocation.getBlock().getType())
						&& (Math.abs(dX) < SpellVesperidysAnticheese.ANTI_BLOCK_ZONE_DISTANCE && Math.abs(dZ) < SpellVesperidysAnticheese.ANTI_BLOCK_ZONE_DISTANCE)) {
						mLocation.getBlock().setType(Material.AIR);
						bullet.getWorld().playSound(mLocation, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.3f, 0.9f);
					}
				}
				mTicks++;
				bullet.teleport(mLocation.clone().add(0, 0, 0));
				if (mTicks >= PATTERN1_BULLET_DURATION + accelStart || mVesperidys.mDefeated || mBoss.isDead() || bullet.getLocation().distance(mVesperidys.mSpawnLoc) > 30) {
					this.cancel();
				}
				if (mTicks >= accelStart && mInnerVelocity < 0.5) {
					mInnerVelocity += 0.05;
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				bullet.remove();
			}
		};
		runnableBullet.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnableBullet);
	}

	private void launchPattern2Bullet(Location detLoc, Vector dir, double accelStart) {
		List<Player> players = PlayerUtils.playersInRange(detLoc, 75, true);

		BlockDisplay bullet = spawnBullet(detLoc, false);
		BukkitRunnable runnableBullet = new BukkitRunnable() {
			final Location mLocation = detLoc.clone();
			final Vector mDir = dir.clone();
			int mTicks = 0;
			double mInnerVelocity = 0;

			@Override
			public void run() {
				// Iterate two times and half the velocity so that way we can have more accurate travel for intersection.
				Location oldLoc = mLocation.clone();

				for (int j = 0; j < 2; j++) {
					mLocation.add(mDir.clone().normalize().multiply(mInnerVelocity));
					BoundingBox box = BoundingBox.of(mLocation, HITBOX, HITBOX, HITBOX);
					for (Player player : players) {
						if (player.getBoundingBox().overlaps(box)) {
							directHit(player);
							MovementUtils.knockAway(oldLoc, player, 1f, 0.5f);
							this.cancel();
							return;
						}
					}

					double dX = mLocation.getX() - mVesperidys.mSpawnLoc.getX();
					double dZ = mLocation.getZ() - mVesperidys.mSpawnLoc.getZ();
					if (mLocation.getBlock().getType() != Material.AIR && !BlockUtils.isMechanicalBlock(mLocation.getBlock().getType())
						&& (Math.abs(dX) < SpellVesperidysAnticheese.ANTI_BLOCK_ZONE_DISTANCE && Math.abs(dZ) < SpellVesperidysAnticheese.ANTI_BLOCK_ZONE_DISTANCE)) {
						mLocation.getBlock().setType(Material.AIR);
						bullet.getWorld().playSound(mLocation, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.3f, 0.9f);
					}
				}
				mTicks++;
				bullet.teleport(mLocation.clone().add(0, 0, 0));
				if (mTicks >= PATTERN2_BULLET_DURATION + accelStart || mVesperidys.mDefeated || mBoss.isDead() || bullet.getLocation().distance(mVesperidys.mSpawnLoc) > 30) {
					this.cancel();
				}
				if (mTicks >= accelStart && mInnerVelocity < 0.3) {
					mInnerVelocity += 0.05;
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				bullet.remove();
			}
		};
		runnableBullet.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnableBullet);
	}

	private void directHit(Player player) {
		mPHit.location(player.getLocation().add(0, 1, 0)).spawnAsBoss();
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1, 2);

		DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DIRECT_HIT_DAMAGE, null, false, false, SPELL_NAME);
	}

	public BlockDisplay spawnBullet(Location loc, boolean small) {
		float size = 0.575f;
		if (small) {
			size /= 2;
		}
		return spawnBullet(loc, size, size);
	}

	public BlockDisplay spawnBullet(Location loc, float width, float height) {
		return EntityUtils.spawnBlockDisplay(loc.getWorld(), loc, BULLET_MATERIAL, width, height, true);
	}
}

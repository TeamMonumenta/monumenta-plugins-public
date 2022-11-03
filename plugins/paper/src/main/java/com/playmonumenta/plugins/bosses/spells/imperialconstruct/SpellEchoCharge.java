package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellEchoCharge extends Spell {

	private static final String ABILITY_NAME = "Echo Charge";
	private static final String MARKER_TAG = "EchoChargeSpawn";
	private static final String TARGET_TAG = "EchoChargeTarget";
	private static final int LINE_LENGTH = 150;
	private final int mRange = 50;
	private static final int BOX_SIZE = 8;
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private final int mCooldown = 20 * 10;
	private int mDamage;
	private ChargeUpManager mChargeUp;
	private int mCastTime = (int) (20 * 7.5);
	private int mExecutionTime = (int) (20 * 3.5);
	private final Location[] mSpawnLocations = new Location[3];
	private final Location[] mTargetLocations = new Location[3];

	public SpellEchoCharge(Plugin plugin, LivingEntity boss, int castTime, int executionTime, int damage) {
		mPlugin = plugin;
		mBoss = boss;
		mCastTime = castTime;
		mExecutionTime = executionTime;
		mDamage = damage;
		mChargeUp = new ChargeUpManager(mBoss, mCastTime, ChatColor.GOLD + "Casting " + ChatColor.YELLOW + ABILITY_NAME,
			BarColor.YELLOW, BarStyle.SOLID, mRange);
	}

	@Override
	public void run() {
		mChargeUp.setTime(0);

		for (Entity e : mBoss.getNearbyEntities(mRange, 10, mRange)) {
			Set<String> tags = e.getScoreboardTags();
			if (tags.contains(MARKER_TAG + "1")) {
				mSpawnLocations[0] = e.getLocation();
			} else if (tags.contains(MARKER_TAG + "2")) {
				mSpawnLocations[1] = e.getLocation();
			} else if (tags.contains(MARKER_TAG + "3")) {
				mSpawnLocations[2] = e.getLocation();
			} else if (tags.contains(TARGET_TAG + "1")) {
				mTargetLocations[0] = e.getLocation();
			} else if (tags.contains(TARGET_TAG + "2")) {
				mTargetLocations[1] = e.getLocation();
			} else if (tags.contains(TARGET_TAG + "3")) {
				mTargetLocations[2] = e.getLocation();
			}
		}

		HashMap<Location, Location> lines = new HashMap<>();
		lines.put(mSpawnLocations[0], mTargetLocations[0]);
		lines.put(mSpawnLocations[1], mTargetLocations[1]);
		lines.put(mSpawnLocations[2], mTargetLocations[2]);
		List<Location> lineKeysArray = new ArrayList<>(lines.keySet());
		Collections.shuffle(lineKeysArray);

		List<Player> nearbyPlayers = PlayerUtils.playersInRange(mBoss.getLocation(), mRange, true);
		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				int ticks = mChargeUp.getTime();
				for (Player target : nearbyPlayers) {
					if (ticks % 8 == 0) {
						target.getWorld().playSound(target.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 0.75f, 0.5f + (ticks / 80f) * 1.5f);
					} else if (ticks % 8 == 2) {
						mBoss.getLocation().getWorld().playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 1.0f, 0.5f + (ticks / 80f) * 1.5f);
					} else if (ticks % 8 == 4) {
						target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.75f, 0.5f + (ticks / 100f) * 1.5f);
					} else if (ticks % 8 == 6) {
						mBoss.getLocation().getWorld().playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 1.0f, 0.5f + (ticks / 100f) * 1.5f);
					}
				}

				if (mChargeUp.nextTick()) {
					rush(lines.get(lineKeysArray.get(1)), lineKeysArray.get(1));
					rush(lines.get(lineKeysArray.get(2)), lineKeysArray.get(2));
					this.cancel();
					mChargeUp.setTitle(ChatColor.GOLD + "Casting " + ChatColor.YELLOW + ABILITY_NAME);
					// Execute ability after charging
					BukkitRunnable runnable = new BukkitRunnable() {
						int mT = 0;
						@Override
						public synchronized void cancel() {
							super.cancel();
							mChargeUp.reset();
							mChargeUp.setTitle(ChatColor.GOLD + "Channeling " + ChatColor.YELLOW + ABILITY_NAME);
						}

						@Override
						public void run() {
							double progress = 1 - ((double) mT / (double) mExecutionTime);
							mChargeUp.setProgress(progress);
							if (progress > 0.01 && progress < 0.95) {
								for (Player target : nearbyPlayers) {
									if (progress % 8 == 0) {
										target.getWorld().playSound(target.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 0.75f, 0.5f + (ticks / 80f) * 1.5f);
									} else if (progress % 8 == 2) {
										mBoss.getLocation().getWorld().playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 1.0f, 0.5f + (ticks / 80f) * 1.5f);
									} else if (progress % 8 == 4) {
										target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.75f, 0.5f + (ticks / 100f) * 1.5f);
									} else if (progress % 8 == 6) {
										mBoss.getLocation().getWorld().playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 1.0f, 0.5f + (ticks / 100f) * 1.5f);
									}
								}
								telegraphParticles(0, lines, lineKeysArray);
							}
							if (mT >= mExecutionTime) {
								rush(lines.get(lineKeysArray.get(0)), lineKeysArray.get(0));
								this.cancel();
								mActiveRunnables.remove(this);
							}
							mT++;
						}
					};
					runnable.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(runnable);
				}
				if (mChargeUp.getTime() < mCastTime) {
					if (mChargeUp.getTime() % 5 == 0) {
						telegraphParticles(1, lines, lineKeysArray);
						telegraphParticles(2, lines, lineKeysArray);
					}
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return mCooldown + mExecutionTime + 20 * 3;
	}

	private void telegraphParticles(int index, HashMap<Location, Location> lines, List<Location> lineKeysArray) {
		Location startLoc1 = lines.get(lineKeysArray.get(index));
		Location endLoc1 = lineKeysArray.get(index);
		Vector baseVector1 = endLoc1.clone().subtract(startLoc1).toVector();
		baseVector1 = baseVector1.normalize().multiply(0.3);
		BoundingBox chargeBox1 = BoundingBox.of(startLoc1, 6, 2, 6);
		World world = mBoss.getWorld();

		for (int i = 0; i < LINE_LENGTH; i++) {
			chargeBox1.shift(baseVector1);
			if (i % 4 == 0) {
				world.spawnParticle(Particle.ELECTRIC_SPARK, chargeBox1.getCenter().toLocation(world), 6, 3.5, 0.1, 3.5, 0.01);
				world.spawnParticle(Particle.WAX_ON, chargeBox1.getCenter().toLocation(world), 2, 3.5, 0.1, 3.5, 0.01);
			}
		}

		Location telegraphLine1 = startLoc1.clone();
		Location telegraphLine2 = startLoc1.clone();
		telegraphLine1.add(-7.6, 0, 0);
		telegraphLine2.add(7.6, 0, 0);

		Location telegraphLineEnd1 = endLoc1.clone();
		Location telegraphLineEnd2 = endLoc1.clone();
		telegraphLineEnd1.add(-7.6, 0, 0);
		telegraphLineEnd2.add(7.6, 0, 0);


		// Start 1 Spot
		world.spawnParticle(Particle.ELECTRIC_SPARK, telegraphLine1, 1, 0.35, 0.5, 0.35, 0.05);
		world.spawnParticle(Particle.WAX_ON, telegraphLine1, 1, 0.35, 0.5, 0.35, 0.05);
		world.spawnParticle(Particle.ELECTRIC_SPARK, telegraphLine1, 1, 0.35, 0.5, 0.35, 0.05);
		world.spawnParticle(Particle.WAX_ON, telegraphLine1, 1, 0.35, 0.5, 0.35, 0.05);

		// Start 2 Spot
		world.spawnParticle(Particle.ELECTRIC_SPARK, telegraphLine2, 1, 0.35, 0.5, 0.35, 0.05);
		world.spawnParticle(Particle.WAX_ON, telegraphLine2, 1, 0.35, 0.5, 0.35, 0.05);
		world.spawnParticle(Particle.ELECTRIC_SPARK, telegraphLine2, 1, 0.35, 0.5, 0.35, 0.05);
		world.spawnParticle(Particle.WAX_ON, telegraphLine2, 1, 0.35, 0.5, 0.35, 0.05);

		// End 1 Spot
		world.spawnParticle(Particle.ELECTRIC_SPARK, telegraphLineEnd1, 1, 0.35, 0.5, 0.35, 0.05);
		world.spawnParticle(Particle.WAX_ON, telegraphLineEnd1, 1, 0.35, 0.5, 0.35, 0.05);
		world.spawnParticle(Particle.ELECTRIC_SPARK, telegraphLineEnd1, 1, 0.35, 0.5, 0.35, 0.05);
		world.spawnParticle(Particle.WAX_ON, telegraphLineEnd1, 1, 0.35, 0.5, 0.35, 0.05);

		// End 2 Spot
		world.spawnParticle(Particle.ELECTRIC_SPARK, telegraphLineEnd2, 1, 0.35, 0.5, 0.35, 0.05);
		world.spawnParticle(Particle.WAX_ON, telegraphLineEnd2, 1, 0.35, 0.5, 0.35, 0.05);
		world.spawnParticle(Particle.ELECTRIC_SPARK, telegraphLineEnd2, 1, 0.35, 0.5, 0.35, 0.05);
		world.spawnParticle(Particle.WAX_ON, telegraphLineEnd2, 1, 0.35, 0.5, 0.35, 0.05);

		new PPLine(Particle.ELECTRIC_SPARK, telegraphLine1, telegraphLineEnd1).shiftStart(0.75).countPerMeter(1).minParticlesPerMeter(0).delta(0.1).extra(0.03).spawnAsBoss();
		new PPLine(Particle.WAX_ON, telegraphLine1, telegraphLineEnd1).shiftStart(0.75).countPerMeter(1).minParticlesPerMeter(0).delta(0.1).extra(0.03).spawnAsBoss();
		new PPLine(Particle.ELECTRIC_SPARK, telegraphLine2, telegraphLineEnd2).shiftStart(0.75).countPerMeter(1).minParticlesPerMeter(0).delta(0.1).extra(0.03).spawnAsBoss();
		new PPLine(Particle.WAX_ON, telegraphLine2, telegraphLineEnd2).shiftStart(0.75).countPerMeter(1).minParticlesPerMeter(0).delta(0.1).extra(0.03).spawnAsBoss();

		new PPLine(Particle.ELECTRIC_SPARK, telegraphLineEnd1.clone().add(0, 1.5, 0), telegraphLine1.clone().add(0, 1.5, 0)).shiftStart(0.75).countPerMeter(1).minParticlesPerMeter(0).delta(0.1).extra(0.03).spawnAsBoss();
		new PPLine(Particle.WAX_ON, telegraphLineEnd1.clone().add(0, 1.5, 0), telegraphLine1.clone().add(0, 1.5, 0)).shiftStart(0.75).countPerMeter(1).minParticlesPerMeter(0).delta(0.1).extra(0.03).spawnAsBoss();
		new PPLine(Particle.ELECTRIC_SPARK, telegraphLineEnd2.clone().add(0, 1.5, 0), telegraphLine2.clone().add(0, 1.5, 0)).shiftStart(0.75).countPerMeter(1).minParticlesPerMeter(0).delta(0.1).extra(0.03).spawnAsBoss();
		new PPLine(Particle.WAX_ON, telegraphLineEnd2.clone().add(0, 1.5, 0), telegraphLine2.clone().add(0, 1.5, 0)).shiftStart(0.75).countPerMeter(1).minParticlesPerMeter(0).delta(0.1).extra(0.03).spawnAsBoss();
	}

	private void rush(Location startLoc, Location endLoc) {
		World world = mBoss.getWorld();
		List<Player> nearbyPlayers = PlayerUtils.playersInRange(mBoss.getLocation(), 50, true);
		BoundingBox chargeBox = BoundingBox.of(startLoc, BOX_SIZE, 25, BOX_SIZE);
		for (Player p : nearbyPlayers) {
			p.playSound(startLoc, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.HOSTILE, 1.2f, 0.6f);
		}
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				Vector baseVector = endLoc.clone().subtract(startLoc).toVector();
				baseVector = baseVector.normalize().multiply(0.3);
				// Charge Telegraph
				if (mTicks > 0 && mTicks < 20 * 2) {
					for (int i = 0; i < LINE_LENGTH; i++) {
						chargeBox.shift(baseVector);
						world.spawnParticle(Particle.EXPLOSION_NORMAL, chargeBox.getCenter().toLocation(world), 1, 4, 3, 4, 0.05);
						world.spawnParticle(Particle.ELECTRIC_SPARK, chargeBox.getCenter().toLocation(world), 2, 4, 3, 4, 0.05);
						for (Player player: nearbyPlayers) {
							if (chargeBox.contains(player.getLocation().toVector())) {
								DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MELEE, mDamage, null, false, true);
							}
						}
					}
				}
				// Charge execute
				if (mTicks >= 20 * 2) {
					for (int i = 0; i < LINE_LENGTH; i++) {
						chargeBox.shift(baseVector);
						world.spawnParticle(Particle.VILLAGER_ANGRY, chargeBox.getCenter().toLocation(world), 25, 5, 3, 5, 0.05);
					}
					this.cancel();
				}
				mTicks += 1;
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}
}

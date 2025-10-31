package com.playmonumenta.plugins.bosses.spells.exalted;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellAxtalGroundSurge extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mCount;
	private final int mRange;
	private boolean mCooldown = false;
	private final String SPELL_NAME = "Ground Surge";

	public SpellAxtalGroundSurge(Plugin plugin, LivingEntity boss, int count, int range) {
		mPlugin = plugin;
		mBoss = boss;
		mCount = count;
		mRange = range;
	}

	@Override
	public void run() {
		if (!mCooldown) {
			mCooldown = true;
			new BukkitRunnable() {
				@Override
				public void run() {
					mCooldown = false;
				}
			}.runTaskLater(mPlugin, 12 * 20);
			return;
		}

		World world = mBoss.getWorld();
		Location bossLoc = mBoss.getLocation();
		world.playSound(bossLoc, Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 2f, 0.6f);
		LivingEntity kaulstatue = (LivingEntity) LibraryOfSoulsIntegration.summon(bossLoc, "ExAxtalKaulStatue");
		if (kaulstatue == null) {
			return;
		}
		kaulstatue.setRotation(bossLoc.getYaw(), bossLoc.getPitch());
		// bobbing + increase height by 3 blocks over 30 ticks
		Location statueLoc = mBoss.getLocation();
		double statueGround = statueLoc.getY();
		BukkitRunnable statueMovement = new BukkitRunnable() {
			int mTicksElapsed = 0;
			double mRadian = 0;

			@Override
			public void run() {

				if (mTicksElapsed < 30) {
					statueLoc.add(0, 0.1, 0);
				}
				// bobbing
				kaulstatue.teleport(statueLoc.clone().add(0, FastMath.sin(mRadian) * 0.1, 0));
				mRadian += Math.PI / 20D; // Finishes a sin bob in (20 * 2) ticks
				mTicksElapsed++;
			}
		};
		statueMovement.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(statueMovement);
		// select targets after 2 seconds (40t), launch "count" times
		BukkitRunnable attack = new BukkitRunnable() {
			int mAttackCount = 0;

			@Override
			public void run() {
				if (mAttackCount < mCount) {
					mAttackCount++;
					List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mRange, true);
					Collections.shuffle(players);
					int targetCount = (int) Math.ceil(players.size() / 3.0);
					List<Player> targets = players.subList(0, Math.min(players.size(), targetCount));
					BukkitRunnable warning = new BukkitRunnable() {
						int mTicks = 0;
						float mPitch = 0;

						@Override
						public void run() {
							kaulstatue.setRotation(bossLoc.getYaw(), bossLoc.getPitch());
							Location mLoc = kaulstatue.getLocation();
							Location pLoc = mLoc.clone();
							pLoc.setY(statueGround);
							if (mTicks >= 20 * 2) {
								launch(world, pLoc, targets, players);
								this.cancel();
								return;
							}
							if (mTicks % 2 == 0) {
								world.playSound(mLoc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 3, mPitch);
							}
							new PartialParticle(Particle.BLOCK_CRACK, pLoc, 8, 0.4, 0.1, 0.4, 0.25, Material.COARSE_DIRT.createBlockData()).spawnAsEntityActive(mBoss);
							new PartialParticle(Particle.SMOKE_LARGE, pLoc, 2, 0.25, 0.1, 0.25, 0.25).spawnAsEntityActive(mBoss);

							mTicks++;
							mPitch = (float) (mPitch + 0.2);
						}
					};
					warning.runTaskTimer(mPlugin, 2 * 20, 1);
				} else {
					world.playSound(kaulstatue.getLocation(), Sound.ENTITY_BLAZE_DEATH, SoundCategory.HOSTILE, 1f, 0.75f);
					new PartialParticle(Particle.SMOKE_LARGE, kaulstatue.getLocation(), 10, 0.25, 0.1, 0.25, 0.25).spawnAsEntityActive(mBoss);
					kaulstatue.remove();
					this.cancel();
				}
			}
		};
		attack.runTaskTimer(mPlugin, 20 * 2, 20 * 4);
	}

	// copied from kaul ground surge
	private void launch(World world, Location nloc, List<Player> targets, List<Player> players) {
		for (Player target : targets) {
			Vector dir = LocationUtils.getDirectionTo(target.getLocation(), nloc).setY(0).normalize().multiply(1.1);

			BukkitRunnable runnable = new BukkitRunnable() {
				int mInnerTicks = 0;
				final BoundingBox mBox = BoundingBox.of(nloc, 0.65, 0.65, 0.65);

				@Override
				public void run() {
					mInnerTicks++;
					mBox.shift(dir);
					Location bLoc = mBox.getCenter().toLocation(world);
					if (bLoc.getBlock().getType().isSolid()) {
						bLoc.add(0, 1, 0);
						if (bLoc.getBlock().getType().isSolid()) {
							this.cancel();
							bLoc.subtract(0, 1, 0);
						}
					}

					if (!bLoc.subtract(0, 1, 0).getBlock().getType().isSolid()) {
						bLoc.subtract(0, 1, 0);
						if (!bLoc.getBlock().getType().isSolid()) {
							bLoc.subtract(0, 1, 0);
							if (!bLoc.getBlock().getType().isSolid()) {
								this.cancel();
							}
						}
					}
					bLoc.add(0, 0.5, 0);

					if (mInnerTicks >= 45) {
						this.cancel();
					}

					world.playSound(bLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.75f, 1);
					new PartialParticle(Particle.BLOCK_CRACK, bLoc, 20, 0.5, 0.5, 0.5, 0.25, Material.COARSE_DIRT.createBlockData()).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.FLAME, bLoc, 15, 0.5, 0.5, 0.5, 0.075).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.LAVA, bLoc, 2, 0.5, 0.5, 0.5, 0.25).spawnAsEntityActive(mBoss);
					for (Player player : players) {
						if (player.getBoundingBox().overlaps(mBox)) {
							this.cancel();
							DamageUtils.damage(mBoss, player, DamageEvent.DamageType.BLAST, 50, null, false, true, SPELL_NAME);
							player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 20, 2));
							MovementUtils.knockAway(mBoss.getLocation(), player, 0.3f, 1f);
							new PartialParticle(Particle.SMOKE_LARGE, bLoc, 20, 0, 0, 0, 0.2).spawnAsEntityActive(mBoss);
							new PartialParticle(Particle.FLAME, bLoc, 75, 0, 0, 0, 0.25).spawnAsEntityActive(mBoss);
							world.playSound(bLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 1);

							// Send surges to all other players now.
							if (players.size() <= 1) {
								break;
							}
							// Find a random other player that is not equal to the current target
							Player rPlayer = target;
							while (rPlayer.getUniqueId().equals(target.getUniqueId())) {
								rPlayer = players.get(FastUtils.RANDOM.nextInt(players.size()));
							}
							Player tPlayer = rPlayer;
							BukkitRunnable runnable = new BukkitRunnable() {
								Player mTPlayer = tPlayer;
								final BoundingBox mBox = BoundingBox.of(bLoc, 0.4, 0.4, 0.4);
								int mTicks = 0;
								int mHits = 0;
								final List<UUID> mHit = new ArrayList<>();

								@Override
								public void run() {
									mTicks++;
									Location innerBoxLoc = mBox.getCenter().toLocation(world);
									Vector dir = LocationUtils.getDirectionTo(mTPlayer.getLocation(), innerBoxLoc).setY(0).normalize();
									mBox.shift(dir.clone().multiply(0.7));
									if (innerBoxLoc.getBlock().getType().isSolid()) {
										innerBoxLoc.add(0, 1, 0);
										if (innerBoxLoc.getBlock().getType().isSolid()) {
											this.cancel();
											innerBoxLoc.subtract(0, 1, 0);
										}
									}
									if (!innerBoxLoc.subtract(0, 1, 0).getBlock().getType().isSolid()) {
										innerBoxLoc.subtract(0, 1, 0);
										if (!innerBoxLoc.getBlock().getType().isSolid()) {
											innerBoxLoc.subtract(0, 1, 0);
											if (!innerBoxLoc.getBlock().getType().isSolid()) {
												this.cancel();
											}
										}
									}
									innerBoxLoc.add(0, 1, 0);
									world.playSound(innerBoxLoc, Sound.BLOCK_STONE_BREAK, SoundCategory.HOSTILE, 0f, 1);
									//Have particles with collision show only for the player who's targeted.
									//This is to prevent lag from the numerous other surges that have these same
									//Particles
									new PartialParticle(Particle.BLOCK_CRACK, innerBoxLoc, 8, 0.2, 0.2, 0.2, 0.25, Material.COARSE_DIRT.createBlockData()).spawnAsEntityActive(mBoss);
									new PartialParticle(Particle.FLAME, innerBoxLoc, 6, 0.2, 0.2, 0.2, 0.075).spawnAsEntityActive(mBoss);
									new PartialParticle(Particle.LAVA, innerBoxLoc, 1, 0.2, 0.2, 0.2, 0.25).spawnAsEntityActive(mBoss);
									for (Player surgePlayer : players) {
										if (surgePlayer.getBoundingBox().overlaps(mBox)
											&& !surgePlayer.getUniqueId().equals(player.getUniqueId())
											&& !mHit.contains(surgePlayer.getUniqueId())) {
											mHit.add(surgePlayer.getUniqueId());
											DamageUtils.damage(mBoss, surgePlayer, DamageEvent.DamageType.BLAST, 50, null, false, true, SPELL_NAME);
											surgePlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 20, 2));
											MovementUtils.knockAway(nloc, player, 0.3f, 1f);
											new PartialParticle(Particle.SMOKE_LARGE, innerBoxLoc, 10, 0, 0, 0, 0.2).spawnAsEntityActive(mBoss);
											new PartialParticle(Particle.FLAME, innerBoxLoc, 50, 0, 0, 0, 0.25).spawnAsEntityActive(mBoss);
											world.playSound(innerBoxLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 1.25f);
											mHits++;
											mTicks = 0;
											if (mHits < players.size() && mHits <= 2) {
												int attempts = 0;
												mTPlayer = players.get(FastUtils.RANDOM.nextInt(players.size()));
												while (mHit.contains(mTPlayer.getUniqueId())) {
													//A rare case can occur where the loop has gone through all of the possible
													//players, but they have been hit. Add an attempt integer to make sure that
													//it does not cause an infinite loop.
													if (attempts < 5) {
														mTPlayer = players.get(FastUtils.RANDOM.nextInt(players.size()));
														attempts++;
													} else {
														this.cancel();
														break;
													}
												}
											} else {
												this.cancel();
											}
										}
									}
									if (mTicks >= 20 * 1.25) {
										this.cancel();
									}
								}

							};
							runnable.runTaskTimer(mPlugin, 0, 1);
							mActiveRunnables.add(runnable);
							break;
						}
					}
				}

			};
			runnable.runTaskTimer(mPlugin, 0, 1);
			mActiveRunnables.add(runnable);
		}
	}

	@Override
	public int cooldownTicks() {
		return 6 * 20;
	}
}

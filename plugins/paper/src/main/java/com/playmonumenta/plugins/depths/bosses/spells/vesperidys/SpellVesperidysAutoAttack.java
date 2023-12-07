package com.playmonumenta.plugins.depths.bosses.spells.vesperidys;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/**
 * Shoots out Lightning Spears. First there is a tell, and then hits.
 * <p>
 * Parameters:
 * DetectionRange - How far to detect players
 * Barrage - Number of "Waves" of attacks.
 * CastTicks - Number of Ticks per wave of attack.
 * Damage - Amount of Magic Damage dealt.
 */
public class SpellVesperidysAutoAttack extends Spell {
	private static final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f);

	private static final Particle.DustOptions WARN_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1f);
	private static final Particle.DustOptions LIGHTNING_COLOR_1 = new Particle.DustOptions(Color.fromRGB(255, 0, 255), 1f);
	private static final Particle.DustOptions LIGHTNING_COLOR_2 = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1f);

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Vesperidys mVesperidys;
	private final double mRange;
	private final int mBarrages;
	private final int mLightningCastTicks;
	private final int mEarthCastTicks;
	private final int mVoidCastTicks;
	private final int mCooldown;
	private final int mMaxTargets;

	private int mTicks = 0;
	private int mSlashTicks = 0;

	private final PartialParticle mPWarn;
	private final PartialParticle mPLightning1;
	private final PartialParticle mPLightning2;
	private final PartialParticle mPSmoke;
	private final PartialParticle mPElectricSpark;
	private final PartialParticle mPFireworkSpark;
	private final PartialParticle mPCritMagic;

	private final PartialParticle mPRed;
	private final PartialParticle mPHit;
	private final PartialParticle mPHitCone1;
	private final PartialParticle mPHitCone2;

	private final double DAMAGE = 55;

	public SpellVesperidysAutoAttack(Plugin plugin, LivingEntity boss, Vesperidys vesperidys, double detectionRange, int maxTargets, int barrages, int lightningCastTicks, int earthCastTicks, int voidCastTicks, int cooldown) {
		mPlugin = plugin;
		mBoss = boss;
		mVesperidys = vesperidys;
		mRange = detectionRange;
		mMaxTargets = maxTargets;
		mBarrages = barrages;
		mLightningCastTicks = lightningCastTicks;
		mEarthCastTicks = earthCastTicks;
		mVoidCastTicks = voidCastTicks;
		mCooldown = cooldown;

		mPWarn = new PartialParticle(Particle.REDSTONE, mBoss.getLocation(), 1, 0.25, 0.25, 0.25, 0, WARN_COLOR);
		mPLightning1 = new PartialParticle(Particle.REDSTONE, mBoss.getLocation(), 1, 0.25, 0.25, 0.25, 0, LIGHTNING_COLOR_1);
		mPLightning2 = new PartialParticle(Particle.REDSTONE, mBoss.getLocation(), 1, 0.25, 0.25, 0.25, 0, LIGHTNING_COLOR_2);
		mPSmoke = new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation(), 1, 0.25, 0.25, 0.25, 0);
		mPElectricSpark = new PartialParticle(Particle.ELECTRIC_SPARK, mBoss.getLocation(), 1, 0.25, 0.25, 0.25, 0);
		mPFireworkSpark = new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation(), 1, 0.25, 0.25, 0.25, 0);
		mPCritMagic = new PartialParticle(Particle.CRIT_MAGIC, mBoss.getLocation(), 1, 0.25, 0.25, 0.25, 0);

		mPRed = new PartialParticle(Particle.REDSTONE, mBoss.getLocation(), 1, 0.1, 0.1, 0.1, 0.1, RED);
		mPHitCone1 = new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation(), 1, 0.1, 0.1, 0.1, 0.1);
		mPHitCone2 = new PartialParticle(Particle.CRIT_MAGIC, mBoss.getLocation(), 2, 0.1, 0.1, 0.1, 0.1);
		mPHit = new PartialParticle(Particle.CRIT_MAGIC, mBoss.getLocation(), 20, 0.25, 0.25, 0.25, 0.25);
	}

	@Override
	public void run() {
		if (!isRunning()) {
			mTicks += BossAbilityGroup.PASSIVE_RUN_INTERVAL_DEFAULT;
			mSlashTicks += BossAbilityGroup.PASSIVE_RUN_INTERVAL_DEFAULT;
		}

		if (mTicks >= mCooldown
			&& !isRunning()
			&& FastUtils.randomIntInRange(0, 5 * 20) + mTicks >= 5 * 20 + mCooldown) {
			mTicks = 0;

			if (FastUtils.randomIntInRange(0, 1) == 0) {
				lightningSpear();
			} else {
				earthenRupture();
			}
		}

		Player nearestPlayer = EntityUtils.getNearestPlayer(mBoss.getLocation(), Vesperidys.detectionRange);

		if (mSlashTicks >= mCooldown
			&& !isRunning()
			&& nearestPlayer != null
			&& nearestPlayer.getLocation().distance(mBoss.getLocation()) <= 10) {
			mSlashTicks = 0;
			voidSlash(nearestPlayer);
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	private void voidSlash(Player target) {
		List<Player> targets = EntityUtils.getNearestPlayers(mBoss.getLocation(), 100);
		Collections.shuffle(targets);
		Vector dir = LocationUtils.getDirectionTo(target.getLocation(), mBoss.getLocation());
		Location tloc = mBoss.getLocation().setDirection(dir);

		BukkitRunnable runnableA = new BukkitRunnable() {
			int mT = 0;
			float mPitch = 0.5f;
			double mAngle = 0;

			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					return;
				}

				Vector v;
				double anglePerTick = 20;
				for (double r = 0; r <= 8; r += 0.5) {
					double resolution = 90.0 / (2 * (r + 1.0));
					double[] angleRange = {-135, 135};

					for (double angle = mAngle; angle <= mAngle + anglePerTick; angle += resolution) {
						double degree = (angle % (angleRange[1] - angleRange[0])) + angleRange[0];

						double radian1 = Math.toRadians(degree);
						v = new Vector(Math.cos(radian1) * r, 0.8, Math.sin(radian1) * r);
						v = VectorUtils.rotateXAxis(v, 0);
						v = VectorUtils.rotateYAxis(v, tloc.getYaw() + 90);

						Location loc = mBoss.getLocation().clone().add(v);
						if (loc.getBlock().getType() == Material.AIR) {
							mPRed.location(loc).spawnAsBoss();
						} else {
							mPRed.location(loc.add(0, 0.5, 0)).spawnAsBoss();
						}
					}
				}
				mAngle += anglePerTick;

				if (mT % (mVoidCastTicks / 4) == 0) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_VILLAGER_WORK_WEAPONSMITH, SoundCategory.HOSTILE, 3, mPitch);
					mPitch += 0.2f;
				}

				if (mT > mVoidCastTicks) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.75f, 0.8f);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 0.75f, 0f);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 0.75f, 0.5f);

					Vector vec;
					List<Player> hitPlayers = new ArrayList<>();

					for (double r1 = 0; r1 <= 8; r1 += 0.5) {
						for (double degree1 = -135; degree1 < 135; degree1 += 10) {
							double radian2 = Math.toRadians(degree1);
							vec = new Vector(Math.cos(radian2) * r1, 0.8, Math.sin(radian2) * r1);
							vec = VectorUtils.rotateXAxis(vec, 0);
							vec = VectorUtils.rotateYAxis(vec, tloc.getYaw() + 90);

							Location l1 = mBoss.getLocation().clone().add(vec);
							mPHitCone1.location(l1).spawnAsBoss();
							mPHitCone2.location(l1).spawnAsBoss();
							BoundingBox box1 = BoundingBox.of(l1, 0.4, 4, 0.4);

							for (Player player : targets) {
								if (player.getBoundingBox().overlaps(box1) && !hitPlayers.contains(player)) {
									// Damage + KB in the function
									mPHit.location(player.getLocation()).spawnAsBoss();
									hitSlash(player, mBoss.getLocation());
									hitPlayers.add(player);
								}
							}
						}
					}
					this.cancel();
					return;
				}

				mT++;
			}
		};
		runnableA.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnableA);
	}

	@Override
	public void onDeath(@Nullable EntityDeathEvent event) {
		cancel();
	}

	private void earthenRupture() {
		List<Player> hitPlayers = new ArrayList<>();

		// Select all players
		List<Player> players = PlayerUtils.playersInRange(mVesperidys.mSpawnLoc, Vesperidys.detectionRange, true);
		ArrayList<Vesperidys.Platform> platforms = new ArrayList<>();
		for (Player p : players) {
			Vesperidys.Platform platform = mVesperidys.mPlatformList.getPlatformNearestToEntity(p);
			if (!platforms.contains(platform)) {
				platforms.add(platform);
			}
		}

		if (platforms.size() < mMaxTargets) {
			int diff = mMaxTargets - platforms.size();
			List<Vesperidys.Platform> randomPlatforms = mVesperidys.mPlatformList.getRandomPlatforms(platforms, diff);
			platforms.addAll(randomPlatforms);
		}

		BukkitRunnable runnableA = new BukkitRunnable() {
			int mRunnableTicks = 0;

			@Override
			public void run() {
				if (mRunnableTicks >= mEarthCastTicks) {
					this.cancel();
					return;
				}

				mRunnableTicks += 1;

				if (mRunnableTicks >= mEarthCastTicks) {
					for (Vesperidys.Platform platform : platforms) {
						mBoss.getWorld().playSound(platform.getCenter(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1f, 1f);

						for (Player player : platform.getPlayersOnPlatform()) {
							if (!hitPlayers.contains(player)) {
								hitEarth(player, platform.getCenter());
								hitPlayers.add(player);
							}
						}

						for (Block block : platform.mBlocks) {
							Location loc = block.getLocation().add(0.5, 1.2, 0.5);
							new PartialParticle(Particle.SMOKE_NORMAL, loc, 1, 0.15, 0.15, 0.15, 0).spawnAsEntityActive(mBoss);
							if (FastUtils.randomIntInRange(0, 2) == 0) {
								// 1/3 chance for particle
								if (FastUtils.randomIntInRange(0, 2) == 0) {
									new PartialParticle(Particle.BLOCK_DUST, loc, 1, 0.15, 0.1, 0.15, 0.25, Material.BLACK_CONCRETE.createBlockData()).spawnAsEntityActive(mBoss);
								} else {
									new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 1, 0.15, 0.1, 0.15, 0.25).spawnAsEntityActive(mBoss);
								}
							}

							if (FastUtils.randomIntInRange(0, 10) == 0) {
								// 1/10 chance for particle
								new PartialParticle(Particle.LAVA, loc, 1, 0.15, 0.1, 0.15, 0.25).spawnAsEntityActive(mBoss);
							}
						}
					}
				} else {
					for (Vesperidys.Platform platform : platforms) {
						if (mRunnableTicks % 5 == 0) {
							mBoss.getWorld().playSound(platform.getCenter(), Sound.BLOCK_NETHER_BRICKS_HIT, SoundCategory.HOSTILE, 1, 0.9f);
						}

						if (mRunnableTicks % 2 == 0) {
							for (Block block : platform.mBlocks) {
								if (FastUtils.randomIntInRange(0, 1) == 0) {
									if (FastUtils.randomIntInRange(0, 100) == 0) {
										new PartialParticle(Particle.EXPLOSION_NORMAL, block.getLocation().add(0.5, 1.2, 0.5), 1, 0.25, 0.1, 0.25, 0).spawnAsEntityActive(mBoss);
									} else {
										new PartialParticle(Particle.BLOCK_DUST, block.getLocation().add(0.5, 1.2, 0.5), 1, 0.1, 0.1, 0.1, 0, Material.BLACK_CONCRETE.createBlockData())
											.spawnAsEntityActive(mBoss);
									}
								}
							}
						}
					}
				}
			}
		};

		runnableA.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnableA);
	}

	private void lightningSpear() {
		BukkitRunnable runnableA = new BukkitRunnable() {
			int mRunnableTicks = 0;

			@Override
			public void run() {
				if (mRunnableTicks >= mBarrages * mLightningCastTicks) {
					this.cancel();
					return;
				}

				if (mRunnableTicks % mLightningCastTicks == 0) {
					startStrikeSounds(mBoss.getLocation());

					List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mRange, true);
					Collections.shuffle(players);
					int countTargets = 0;
					for (Player player : players) {
						countTargets++;
						if (mMaxTargets < countTargets) {
							break;
						}

						BukkitRunnable runnableB = new BukkitRunnable() {
							int mT = 0;
							final Location mTargetLoc = player.getLocation().add(0, 1.25, 0);

							@Override
							public void run() {
								Vector dir = LocationUtils.getDirectionTo(mTargetLoc, mBoss.getLocation().add(0, 1.25, 0));

								if (mT < mLightningCastTicks) {
									if (mT % 2 == 0) {
										Location loc = mBoss.getLocation().add(0, 1.25, 0);
										for (int i = 0; i < 40; i++) {
											loc.add(dir.clone().multiply(0.75));
											if (FastUtils.randomIntInRange(0, 1) == 0) {
												mPWarn.location(loc).data(new Particle.DustOptions(Color.fromRGB(255, 255, (int) (255.0 * (1 - ((double) mT / mLightningCastTicks)))), 1f)).spawnAsBoss();
											} else {
												mPWarn.location(loc).data(new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1f)).spawnAsBoss();
											}
											mPCritMagic.location(loc).spawnAsBoss();
										}
									}
								} else {
									List<Player> hitPlayers = new ArrayList<>();
									BoundingBox box = BoundingBox.of(mBoss.getLocation().add(0, 1.25, 0), 0.75, 0.75, 0.75);
									mBoss.getWorld().playSound(mBoss.getLocation().add(0, 1.25, 0), Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 2f, 1.75f);
									mBoss.getWorld().playSound(mBoss.getLocation().add(0, 1.25, 0), Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 2f, 0.9f);

									for (int i = 0; i < 40; i++) {
										box.shift(dir.clone().multiply(0.75));
										Location bLoc = box.getCenter().toLocation(mBoss.getWorld());
										if (FastUtils.randomIntInRange(0, 1) == 0) {
											mPLightning1.location(bLoc).spawnAsBoss();
										} else {
											mPLightning2.location(bLoc).spawnAsBoss();
										}
										mPFireworkSpark.location(bLoc).spawnAsBoss();
										mPElectricSpark.location(bLoc).spawnAsBoss();
										for (Player p : players) {
											if (!hitPlayers.contains(p) && p.getBoundingBox().overlaps(box)) {
												hitLightning(p, mBoss.getLocation().add(0, 1.25, 0));
												hitPlayers.add(p);
											}
										}
									}
									this.cancel();
								}

								mT += 2;
							}
						};
						runnableB.runTaskTimer(mPlugin, 0, 2);
						mActiveRunnables.add(runnableB);
					}
				}

				mRunnableTicks += 2;
			}
		};

		runnableA.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnableA);
	}

	// Definitely not stolen from Kaul.
	private void startStrikeSounds(Location strikeLocation) {
		// S: Lightning & distant sparks
		mBoss.getWorld().playSound(
			strikeLocation,
			Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
			SoundCategory.HOSTILE, 5f,
			1.25f
		);
		mBoss.getWorld().playSound(
			strikeLocation,
			Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
			SoundCategory.HOSTILE,
			5f,
			1.5f
		);
		mBoss.getWorld().playSound(
			strikeLocation,
			Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
			SoundCategory.HOSTILE,
			5f,
			1.75f
		);
		mBoss.getWorld().playSound(
			strikeLocation,
			Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR,
			SoundCategory.HOSTILE,
			5f,
			1.75f
		);
	}

	public void hitSlash(Player player, Location origin) {
		BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MELEE, DAMAGE, "Void Slash", mBoss.getLocation());
		MovementUtils.knockAway(origin, player, 0.5f, 0.5f, true);
	}

	public void hitLightning(Player player, Location origin) {
		if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
			player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 2f, 1.5f);
			BukkitRunnable runnableC = new BukkitRunnable() {
				int mT = 0;
				float mVolume = 1f;

				@Override
				public void run() {
					if (mT >= 20) {
						this.cancel();
						return;
					}

					player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, mVolume, 2);

					mT += 4;
					mVolume -= 0.1f;
				}
			};
			runnableC.runTaskTimer(mPlugin, 0, 4);
			mActiveRunnables.add(runnableC);

			mPSmoke.location(player.getLocation().add(0, 1.25, 0)).count(20).extra(1).spawnAsBoss();
			DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE, null, true, false, "Black Lightning");
			MovementUtils.knockAway(origin, player, 0.2f, 0.2f, true);
			mPlugin.mEffectManager.addEffect(player, "LightningSpearVuln", new PercentDamageReceived(15 * 20, 0.3, EnumSet.of(DamageEvent.DamageType.MAGIC)));
		}
	}

	public void hitEarth(Player player, Location origin) {
		DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MELEE, DAMAGE, null, true, false, "Voidquake");
		MovementUtils.knockAway(origin, player, 0.5f, 0.75f, true);
		mPlugin.mEffectManager.addEffect(player, "EarthenRuptureVuln", new PercentDamageReceived(15 * 20, 0.3, EnumSet.of(DamageEvent.DamageType.MELEE)));
	}
}

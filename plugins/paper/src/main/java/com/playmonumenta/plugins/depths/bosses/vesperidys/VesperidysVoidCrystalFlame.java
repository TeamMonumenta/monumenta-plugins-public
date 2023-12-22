package com.playmonumenta.plugins.depths.bosses.vesperidys;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.depths.bosses.spells.vesperidys.SpellVoidCrystalTeleportPassive;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class VesperidysVoidCrystalFlame extends BossAbilityGroup {
	public static final String identityTag = "boss_vesperidysvoidcrystalflame";

	private final Vesperidys mVesperidys;

	private static final int DELAY = 2 * 20;
	private static final int COOLDOWN = 20 * 20;
	private static final int DURATION = 10 * 20;
	private static final double DAMAGE = 40;
	private static final double HEIGHT = 6;
	private static final double RADIUS = 5;
	private static final int ANGLE = 360;

	public static @Nullable VesperidysVoidCrystalFlame deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return construct(plugin, boss);
	}

	public static @Nullable VesperidysVoidCrystalFlame construct(Plugin plugin, LivingEntity boss) {
		// Get nearest entity called Vesperidys.
		Vesperidys vesperidys = null;
		List<LivingEntity> witherSkeletons = EntityUtils.getNearbyMobs(boss.getLocation(), 100, EnumSet.of(EntityType.WITHER_SKELETON));
		for (LivingEntity mob : witherSkeletons) {
			if (mob.getScoreboardTags().contains(Vesperidys.identityTag)) {
				vesperidys = BossUtils.getBossOfClass(mob, Vesperidys.class);
				break;
			}
		}
		if (vesperidys == null) {
			MMLog.warning("VesperidysVoidCrystalFlame: Vesperidys wasn't found! (This is a bug)");
			return null;
		}
		return new VesperidysVoidCrystalFlame(plugin, boss, vesperidys);
	}

	public VesperidysVoidCrystalFlame(Plugin plugin, LivingEntity boss, Vesperidys vesperidys) {
		super(plugin, identityTag, boss);
		mVesperidys = vesperidys;

		Spell spell = new Spell() {

				@Override
				public void run() {

					open();
					new PartialParticle(Particle.FLAME, LocationUtils.getEntityCenter(mBoss), 10, 0.5, 0.5, 0.5).spawnAsBoss();
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2, 1);

					BukkitRunnable shieldWallRunnable = new BukkitRunnable() {
						int mT = -DELAY;
						final Location mLoc = mBoss.getLocation().add(0, -3, 0);
						final Hitbox mHitbox = Hitbox.approximateHollowCylinderSegment(mLoc, HEIGHT, RADIUS - 0.6, RADIUS + 0.6, Math.toRadians(ANGLE) / 2);
						List<LivingEntity> mMobsAlreadyHit = new ArrayList<>();

						@Override
						public synchronized void cancel() {
							super.cancel();
							close();
						}

						@Override
						public void run() {
							mT++;
							Vector vec;

							if (mT == 0) {
								mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ITEM_FIRECHARGE_USE, 2, 0.5f);
							}

							if (mT >= 0) {
								if (mT % 4 == 0) {
									for (double degree = 0; degree < ANGLE; degree += 10) {
										double radian1 = Math.toRadians(degree);
										vec = new Vector(FastUtils.cos(radian1) * RADIUS, 0, FastUtils.sin(radian1) * RADIUS);
										vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

										Location l = mLoc.clone().add(vec);
										for (int y = 0; y < HEIGHT; y++) {
											l.add(0, 1, 0);
											new PartialParticle(Particle.FLAME, l, 1, 0, 0, 0)
												.spawnAsBoss();
										}
									}
								}

								List<? extends LivingEntity> targets = PlayerUtils.playersInRange(mVesperidys.mSpawnLoc, Vesperidys.detectionRange, true);

								List<LivingEntity> entities = targets.stream().filter(e -> mHitbox.intersects(e.getBoundingBox())).collect(Collectors.toList());
								for (LivingEntity le : entities) {
									// This list does not update to the mobs hit this tick until after everything runs
									if (!mMobsAlreadyHit.contains(le)) {
										mMobsAlreadyHit.add(le);

										Location shieldLocation = mLoc.clone();
										shieldLocation.setY(le.getEyeLocation().getY());
										if (le.getEyeLocation().distanceSquared(shieldLocation) < RADIUS * RADIUS) {
											shieldLocation.add(LocationUtils.getDirectionTo(le.getEyeLocation(), shieldLocation).multiply(RADIUS));
										}

										BossUtils.blockableDamage(boss, le, DamageEvent.DamageType.MAGIC, DAMAGE, "Wall Of Fire", shieldLocation);

										MovementUtils.knockAway(mLoc, le, 0.3f, true);


										Location entityLoc = le.getLocation();
										new PartialParticle(Particle.SMOKE_NORMAL, entityLoc, 20, 1, 1, 1)
											.spawnAsBoss();
										mBoss.getWorld().playSound(entityLoc, Sound.BLOCK_FIRE_EXTINGUISH, 2, 1f);
									}
								}

								/*
								 * Compare the two lists of mobs and only remove from the
								 * actual hit tracker if the mob isn't detected as hit this
								 * tick, meaning it is no longer in the shield wall hitbox
								 * and is thus eligible for another hit.
								 */
								List<LivingEntity> mobsAlreadyHitAdjusted = new ArrayList<>();
								for (LivingEntity mob : mMobsAlreadyHit) {
									if (entities.contains(mob)) {
										mobsAlreadyHitAdjusted.add(mob);
									}
								}

								mMobsAlreadyHit = mobsAlreadyHitAdjusted;
								if (mT >= DURATION || EntityUtils.shouldCancelSpells(mBoss)) {
									mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 2, 0.5f);
									this.cancel();
								}
							} else {
								if (mT % 4 == 0) {
									Vector forecastVec;
									for (double degree = 0; degree < ANGLE; degree += 10) {
										double radian1 = Math.toRadians(degree);
										forecastVec = new Vector(FastUtils.cos(radian1) * RADIUS, 0, FastUtils.sin(radian1) * RADIUS);

										Location l = mLoc.clone().add(forecastVec);
										for (int y = 0; y < HEIGHT; y++) {
											l.add(0, 1, 0);
											new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0)
												.data(new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f))
												.spawnAsBoss();
										}
									}
								}
							}
						}

					};
					shieldWallRunnable.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(shieldWallRunnable);
				}

				@Override
				public int cooldownTicks() {
					return COOLDOWN;
				}
			};

		SpellManager activeSpells = new SpellManager(List.of(spell));

		List<Spell> passiveSpells = List.of(
			new SpellVoidCrystalTeleportPassive(mVesperidys, boss)
		);

		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, Vesperidys.detectionRange);
		super.constructBoss(activeSpells, passiveSpells, Vesperidys.detectionRange, null, DELAY);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		Location bossLoc = mBoss.getLocation();
		Vesperidys.Platform targetPlatform = mVesperidys.mPlatformList.getPlatformNearestToEntity(mBoss);

		if (targetPlatform != null) {
			Location centre = targetPlatform.getCenter().clone().add(0, 1, 0);
			int telegraphTicks = 60;

			// Meteor Telegraph
			Location startLoc = LocationUtils.randomLocationInCircle(centre.clone().add(0, 16, 0), 10);
			Vector dir = LocationUtils.getVectorTo(centre, startLoc).normalize().multiply(0.5);
			Location pLoc = startLoc.clone();
			for (int i = 0; i < 40; i++) {
				pLoc.add(dir);

				new PartialParticle(Particle.FLAME, pLoc, 1, 0, 0, 0)
					.extra(0)
					.spawnAsBoss();

				if (pLoc.distance(centre) < 0.5) {
					break;
				}
			}

			centre.getWorld().playSound(centre, Sound.ITEM_FIRECHARGE_USE, 3, 0.5f);
			centre.getWorld().playSound(centre, Sound.ITEM_TOTEM_USE, 3, 2f);

			BukkitRunnable deathRunnable = new BukkitRunnable() {
				int mDeathTicks = 0;

				private double squareX(double value) {
					value = value % 4;

					if (value < 1) {
						return -0.5 + value;
					} else if (value < 2) {
						return 0.5;
					} else if (value < 3) {
						return 2.5 - value;
					} else {
						return -0.5;
					}
				}

				private double squareZ(double value) {
					value = value % 4;

					if (value < 1) {
						return -0.5;
					} else if (value < 2) {
						return -1.5 + value;
					} else if (value < 3) {
						return 0.5;
					} else {
						return 3.5 - value;
					}
				}

				@Override
				public void run() {
					if (mVesperidys.mDefeated) {
						this.cancel();
						return;
					}

					if (mDeathTicks > telegraphTicks) {
						centre.getWorld().playSound(centre, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 3, 1);
						new PPExplosion(Particle.FLAME, centre.clone().add(0, 1.5, 0))
							.extra(1)
							.count(20)
							.spawnAsBoss();
						new PPExplosion(Particle.SOUL_FIRE_FLAME, centre.clone().add(0, 1.5, 0))
							.extra(1)
							.count(20)
							.spawnAsBoss();
						for (Player player : targetPlatform.getPlayersOnPlatform()) {
							DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE * 3, null, true, true, "Starfall");
						}

						targetPlatform.destroy();

						Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
								if (mVesperidys.mPhase < 4 || (mVesperidys.mPhase >= 4 && Math.abs(targetPlatform.mX) <= 1 && Math.abs(targetPlatform.mY) <= 1)) {
									if (mVesperidys.mFullPlatforms) {
										targetPlatform.generateFull();
									} else {
										targetPlatform.generateInner();
									}
								}
							}, 20*20);

						this.cancel();
						return;
					}

					for (int i = 0; i < 8; i++) {
						double value = (double) 4 * mDeathTicks / 40 + 0.5 * i;

						double x = squareX(value) * 7;
						double z = squareZ(value) * 7;

						Location pLoc = centre.clone().add(x, 0, z);

						new PartialParticle(Particle.SOUL_FIRE_FLAME, pLoc, 1, 0, 1, 0)
							.extra(0.1)
							.directionalMode(true)
							.spawnAsBoss();
					}

					new PartialParticle(Particle.SOUL_FIRE_FLAME, bossLoc, 1, 0, 0, 0)
						.extra(0)
						.spawnAsBoss();

					if (mDeathTicks % 5 == 0) {
						double distance = startLoc.distance(centre) * ((double) mDeathTicks / telegraphTicks);
						Vector dir = LocationUtils.getVectorTo(centre, startLoc).normalize().multiply(distance);
						Location pLoc = startLoc.clone().add(dir);

						new PartialParticle(Particle.EXPLOSION_LARGE, pLoc, 1, 0, 0, 0)
							.extra(0)
							.spawnAsBoss();

						pLoc.getWorld().playSound(pLoc, Sound.ENTITY_GENERIC_EXPLODE, 3, 1.4f - 0.4f*((float) mDeathTicks / telegraphTicks));
					}

					if (mDeathTicks % 5 == 0) {
						centre.getWorld().playSound(centre, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.HOSTILE, 3, 1f);

						int radius = mDeathTicks / 5;

						for (int x = -radius; x < radius; x++) {
							for (int z = -radius; z < radius; z++) {
								Location bLoc = centre.clone().add(x, -1, z);
								Block block = bLoc.getBlock();

								if (targetPlatform.mBlocks.contains(block)) {
									for (int y = -4; y < 4; y++) {
										Block blockRelative = block.getRelative(0, y, 0);
										if (blockRelative.getType() != Material.MAGMA_BLOCK && blockRelative.isSolid()) {
											TemporaryBlockChangeManager.INSTANCE.changeBlock(blockRelative, Material.MAGMA_BLOCK, 120 * 20);
										}
									}
								}
							}
						}
					}

					mDeathTicks++;
				}
			};
			deathRunnable.runTaskTimer(mPlugin, 0, 1);
		}
	}

	public void open() {
		if (mBoss instanceof Shulker shulker) {
			shulker.setPeek(1.0f);
		}
	}

	public void close() {
		if (mBoss instanceof Shulker shulker) {
			shulker.setPeek(0.0f);
		}
	}
}

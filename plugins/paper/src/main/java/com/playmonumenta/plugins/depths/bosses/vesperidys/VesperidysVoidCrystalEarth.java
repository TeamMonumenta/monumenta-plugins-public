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
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
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

public class VesperidysVoidCrystalEarth extends BossAbilityGroup {
	public static final String identityTag = "boss_vesperidysvoidcrystalearth";

	private final Vesperidys mVesperidys;

	private static final int DELAY = 2 * 20;
	private static final int TELEGRAPH_TICKS = 3 * 20;
	private static final int COOLDOWN = 10 * 20;
	private static final int DAMAGE = 60;
	private static final double DAMAGE_MULTPLIER = 0.3;

	private double mAbsorbedDamage = 0;
	private int mOpenTicks = 0;

	public static @Nullable VesperidysVoidCrystalEarth deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return construct(plugin, boss);
	}

	public static @Nullable VesperidysVoidCrystalEarth construct(Plugin plugin, LivingEntity boss) {
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
			MMLog.warning("VesperidysBlockPlacerBoss: Vesperidys wasn't found! (This is a bug)");
			return null;
		}
		return new VesperidysVoidCrystalEarth(plugin, boss, vesperidys);
	}

	public VesperidysVoidCrystalEarth(Plugin plugin, LivingEntity boss, Vesperidys vesperidys) {
		super(plugin, identityTag, boss);
		mVesperidys = vesperidys;

		Spell spell = new Spell() {

			@Override
			public void run() {
				Vesperidys.Platform platform = mVesperidys.mPlatformList.getPlatformNearestToEntity(mBoss);

				// Generally should never happen
				if (platform == null) {
					return;
				}

				BukkitRunnable runnable = new BukkitRunnable() {
					int mRunnableTicks = 0;

					@Override
					public void run() {
						if (platform.mBroken) {
							this.cancel();
							return;
						}

						if (mRunnableTicks > TELEGRAPH_TICKS) {
							mBoss.getWorld().playSound(platform.getCenter(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1f, 0.7f);

							for (Player player : platform.getPlayersOnPlatform()) {
								DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MELEE, DAMAGE, null, true, false, "Earth's Wrath");
								MovementUtils.knockAway(platform.getCenter(), player, 0.5f, 0.75f, true);
							}

							for (Block block : platform.mBlocks) {
								if (block.getLocation().getBlockY() == platform.getCenter().getBlockY()) {
									Location loc = block.getLocation().add(0.5, 1.2, 0.5);
									new PartialParticle(Particle.SMOKE_NORMAL, loc, 1, 0.15, 0.15, 0.15, 0).spawnAsEntityActive(mBoss);
									if (FastUtils.randomIntInRange(0, 2) == 0) {
										// 1/3 chance for particle
										if (FastUtils.randomIntInRange(0, 2) == 0) {
											new PartialParticle(Particle.BLOCK_DUST, loc, 1, 0.15, 0.1, 0.15, 0.25, Material.DIRT.createBlockData()).spawnAsEntityActive(mBoss);
										} else {
											new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 1, 0.15, 0.1, 0.15, 0.25).spawnAsEntityActive(mBoss);
										}
									}

									if (FastUtils.randomIntInRange(0, 10) == 0) {
										// 1/10 chance for particle
										new PartialParticle(Particle.LAVA, loc, 1, 0.15, 0.1, 0.15, 0.25).spawnAsEntityActive(mBoss);
									}
								}

								this.cancel();
								return;
							}
						}

						if (mRunnableTicks % 5 == 0) {
							mBoss.getWorld().playSound(platform.getCenter(), Sound.BLOCK_GRAVEL_HIT, SoundCategory.HOSTILE, 1, 0.9f);
						}
						for (Block block : platform.mBlocks) {
							if (block.getLocation().getBlockY() == platform.getCenter().getBlockY()) {
								if (FastUtils.randomIntInRange(0, 2) == 0) {
									if (FastUtils.randomIntInRange(0, 100) == 0) {
										new PartialParticle(Particle.EXPLOSION_NORMAL, block.getLocation().add(0.5, 1.2, 0.5), 1, 0.25, 0.1, 0.25, 0).spawnAsEntityActive(mBoss);
									} else {
										new PartialParticle(Particle.BLOCK_DUST, block.getLocation().add(0.5, 1.2, 0.5), 1, 0.1, 0.1, 0.1, 0, Material.DIRT.createBlockData())
											.spawnAsEntityActive(mBoss);
									}
								}
							}
						}

						mRunnableTicks++;
					}
				};
				runnable.runTaskTimer(mPlugin, 0, 1);
				mActiveRunnables.add(runnable);
			}

			@Override
			public int cooldownTicks() {
				return COOLDOWN;
			}
		};

		SpellManager activeSpells = new SpellManager(List.of(spell));

		List<Spell> passiveSpells = List.of(
			new SpellVoidCrystalTeleportPassive(mVesperidys, boss),
			new Spell() {
				@Override
				public void run() {
					if (mOpenTicks > 0) {
						open();
						mOpenTicks -= 5;
					} else {
						close();
					}
				}

				@Override
				public int cooldownTicks() {
					return 0;
				}
			}
		);

		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, Vesperidys.detectionRange);
		super.constructBoss(activeSpells, passiveSpells, Vesperidys.detectionRange, null, DELAY);
	}

	public boolean absorbDamage(LivingEntity le, DamageEvent event) {
		if (event.getType() != DamageEvent.DamageType.PROJECTILE) {
			double originalDamage = event.getDamage();
			mAbsorbedDamage += originalDamage;

			// Create a new DamageEvent from the EntityDamageEvent with the same damage and damage type but a different damagee
			DamageUtils.damage(event.getSource(), mBoss, event.getType(), originalDamage * DAMAGE_MULTPLIER, null, true, false, "Earthern Wrath");

			World world = mBoss.getWorld();
			Location wrathLoc = LocationUtils.getEntityCenter(mBoss);
			Location otherLoc = LocationUtils.getEntityCenter(le);

			world.playSound(wrathLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 1, 2);
			world.playSound(otherLoc, Sound.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 1, 2);
			new PartialParticle(Particle.TOTEM, otherLoc, 30, 0.1, 0.1, 0.1, 0.6).spawnAsEntityActive(mBoss);

			Location pLoc = otherLoc.clone().add(0, 0.5, 0);
			Vector dir = wrathLoc.toVector().subtract(otherLoc.toVector()).normalize();
			for (int i = 0; i <= wrathLoc.distance(otherLoc); i++) {
				pLoc.add(dir);

				new PartialParticle(Particle.VILLAGER_HAPPY, pLoc, 3, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.CLOUD, pLoc, 6, 0.05, 0.05, 0.05, 0.05).spawnAsEntityActive(mBoss);
			}

			event.setDamage(0);
			le.setNoDamageTicks(10);

			mOpenTicks = 3 * 20;
			open();
			return true;
		}

		return false;
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		World world = mBoss.getWorld();
		Vesperidys.Platform targetPlatform = mVesperidys.mPlatformList.getPlatformNearestToEntity(mBoss);

		if (targetPlatform != null) {
			Location centre = targetPlatform.getCenter().clone().add(0, 1, 0);
			int telegraphTicks = 55;

			BukkitRunnable deathRunnable = new BukkitRunnable() {
				int mDeathTicks = 0;

				@Override
				public synchronized void cancel() {
					super.cancel();
				}

				@Override
				public void run() {
					if (mVesperidys.mDefeated) {
						this.cancel();
					}

					if (mDeathTicks > telegraphTicks) {
						world.playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 10, 0.5f);

						new PPExplosion(Particle.CLOUD, centre.clone().add(0, 1.5, 0))
							.extra(1)
							.count(20)
							.spawnAsBoss();
						new PPExplosion(Particle.REDSTONE, centre.clone().add(0, 1.5, 0))
							.data(new Particle.DustOptions(Color.fromRGB(165, 42, 42), 1.0f))
							.extra(1)
							.count(20)
							.spawnAsBoss();
						for (Player player : targetPlatform.getPlayersOnPlatform()) {
							DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MELEE, Math.max(Math.min(DAMAGE * 4, mAbsorbedDamage * 2), DAMAGE), null, true, true, "Earthen Wrath");
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
						}, 20 * 20);
						this.cancel();
						return;
					}

					if (mDeathTicks % 15 == 0) {
						world.playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.HOSTILE, 10, 0.5f);

						for (Block block : targetPlatform.mBlocks) {
							Location loc = block.getLocation().add(0.5, 1.2, 0.5);
							new PartialParticle(Particle.SMOKE_NORMAL, loc, 1, 0.15, 0.15, 0.15, 0).spawnAsEntityActive(mBoss);
							if (FastUtils.randomIntInRange(0, 2) == 0) {
								// 1/3 chance for particle
								if (FastUtils.randomIntInRange(0, 2) == 0) {
									new PartialParticle(Particle.BLOCK_DUST, loc, 1, 0.15, 0.1, 0.15, 0.25, Material.DIRT.createBlockData()).spawnAsEntityActive(mBoss);
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

					if (mDeathTicks % 5 == 0) {
						int radius = mDeathTicks / 5;

						for (int x = -radius; x < radius; x++) {
							for (int z = -radius; z < radius; z++) {
								Location bLoc = centre.clone().add(x, -1, z);
								Block block = bLoc.getBlock();

								if (targetPlatform.mBlocks.contains(block)) {
									for (int y = -4; y < 4; y++) {
										Block blockRelative = block.getRelative(0, y, 0);
										if (blockRelative.getType() != Material.SOUL_SOIL && blockRelative.isSolid()) {
											TemporaryBlockChangeManager.INSTANCE.changeBlock(blockRelative, Material.SOUL_SOIL, 120 * 20);
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
		if (mBoss instanceof Shulker shulker && shulker.getPeek() < 0.5f) {
			shulker.setPeek(1.0f);
		}
	}

	public void close() {
		if (mBoss instanceof Shulker shulker && shulker.getPeek() > 0.5f) {
			shulker.setPeek(0.0f);
		}
	}
}

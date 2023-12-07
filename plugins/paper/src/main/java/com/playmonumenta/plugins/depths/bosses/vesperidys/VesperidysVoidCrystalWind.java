package com.playmonumenta.plugins.depths.bosses.vesperidys;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.ForceBoss;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellForce;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.depths.bosses.spells.vesperidys.SpellVoidCrystalTeleportPassive;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
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
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class VesperidysVoidCrystalWind extends BossAbilityGroup {
	public static final String identityTag = "boss_vesperidysvoidcrystalwind";

	private final Vesperidys mVesperidys;
	private final Plugin mMonuPlugin;
	private static final double DAMAGE = 60;
	private static final int DELAY = 2 * 20;
	private static final int COOLDOWN = 20 * 20;

	private static final double RADIUS = 2.5;
	private static final double CLOUD_TICKS = 10 * 20;

	public static @Nullable VesperidysVoidCrystalWind deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return construct(plugin, boss);
	}

	public static @Nullable VesperidysVoidCrystalWind construct(Plugin plugin, LivingEntity boss) {
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
		return new VesperidysVoidCrystalWind(plugin, boss, vesperidys);
	}

	public VesperidysVoidCrystalWind(Plugin plugin, LivingEntity boss, Vesperidys vesperidys) {
		super(plugin, identityTag, boss);
		mMonuPlugin = plugin;
		mVesperidys = vesperidys;

		ForceBoss.Parameters p = new ForceBoss.Parameters();
		p.RADIUS = 4;
		p.NEED_PLAYERS = false;

		if (p.TARGETS == EntityTargets.GENERIC_PLAYER_TARGET) {
			p.TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, p.RADIUS, true, EntityTargets.Limit.DEFAULT);
			//by default Force boss hit all the player in range even the players in stealth
		}

		final double currentRadius = p.TARGETS.getRange();
		SpellForce forceSpell = new SpellForce(plugin, boss, (int) currentRadius, p.DURATION, p.COOLDOWN, p.NEED_PLAYERS) {

			@Override
			protected void chargeAuraAction(Location loc) {
				p.PARTICLE_AIR.spawn(boss, loc, currentRadius / 2, currentRadius / 2, currentRadius / 2, 0.05);
			}

			@Override
			protected void chargeCircleAction(Location loc, double radius) {
				p.PARTICLE_CIRCLE.spawn(boss, particle -> new PPCircle(particle, loc, radius).delta(0.25).extra(0.1));
			}

			@Override
			protected void outburstAction(Location loc) {
				p.PARTICLE_EXPLODE.spawn(boss, loc, 0.5, 0, 0.5, 0.8f);
				p.SOUND_EXPLODE.play(loc, 1.0f, 0.7f);
			}

			@Override
			protected void circleOutburstAction(Location loc, double radius) {
				p.PARTICLE_CIRCLE_EXPLODE.spawn(boss, particle -> new PPCircle(particle, loc, radius).delta(0.2).extra(0.2));
			}

			@Override
			protected void dealDamageAction(Location loc) {
				for (LivingEntity target : p.TARGETS.getTargetsList(mBoss)) {
					BossUtils.blockableDamage(mLauncher, target, DamageEvent.DamageType.MELEE, DAMAGE / 3, "Wrath", mBoss.getLocation());

					double distance = target.getLocation().distance(loc);
					if (distance < p.TARGETS.getRange() / 3.0) {
						p.EFFECTS_NEAR.apply(target, boss);
					} else if (distance < (p.TARGETS.getRange() * 2.0) / 3.0) {
						p.EFFECTS_MIDDLE.apply(target, boss);
					} else if (distance < p.TARGETS.getRange()) {
						p.EFFECTS_LIMIT.apply(target, boss);
					}
					p.PARTICLE_HIT.spawn(boss, target.getEyeLocation(), 0.25, 0.5, 0.25, 0);
				}
			}
		};
		SpellVoidCrystalTeleportPassive teleportSpell = new SpellVoidCrystalTeleportPassive(mVesperidys, boss);

		Spell spell = new Spell() {

			@Override
			public void run() {
				teleportSpell.teleportRandomly();

				World world = mBoss.getWorld();
				Location centerLoc = mBoss.getLocation();
				double y = mVesperidys.mSpawnLoc.getBlockY();
				open();

				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					close();
				}, COOLDOWN/2);

				forceSpell.run();

				BukkitRunnable runnable = new BukkitRunnable() {
					int mWrathTicks = 0;

					@Override
					public void run() {
						if (mVesperidys.mDefeated) {
							this.cancel();
							return;
						}

						if (mWrathTicks % 10 == 0) {
							world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.HOSTILE, 2, 1);
						}

						new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 2, 0.25, 0.1, 0.25, 0.25).spawnAsEntityActive(mBoss);
						if (mWrathTicks > 20 * 2) {
							this.cancel();
							world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 2, 1);
							world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2, 1);
							Location loc = mBoss.getLocation();
							loc.setY(y);

							int numProjectiles = 40;

							for (int i = 0; i < numProjectiles; i++) {
								int j = i;
								BukkitRunnable runnable = new BukkitRunnable() {
									final BoundingBox mBox = BoundingBox.of(loc, 0.75, 0.4, 0.75);
									final double mRadian1 = Math.toRadians(((double) j * 360 / numProjectiles));
									final Location mPoint = loc.clone().add(FastUtils.cos(mRadian1) * 0.5, 0, FastUtils.sin(mRadian1) * 0.5);
									final Vector mDir = LocationUtils.getDirectionTo(mPoint, loc).normalize();
									int mTicks = 0;

									@Override
									public void run() {
										if (mVesperidys.mDefeated) {
											this.cancel();
											return;
										}

										mTicks++;
										mBox.shift(mDir.clone().multiply(0.4));
										Location bLoc = mBox.getCenter().toLocation(world);
										new PartialParticle(Particle.CLOUD, bLoc, 1, 0, 0, 0, 0).spawnAsEntityActive(mBoss);
										new PartialParticle(Particle.CRIT_MAGIC, bLoc, 1, 0, 0, 0, 0.25).spawnAsEntityActive(mBoss);

										for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 40, true)) {
											if (player.getBoundingBox().overlaps(mBox)) {
												DamageUtils.damage(mBoss, player, DamageEvent.DamageType.PROJECTILE_SKILL, DAMAGE, null, false, true, "Slipstream");
												MovementUtils.knockAway(centerLoc, player, -0.6f, 0.8f);
											}
										}
										if (mTicks >= 20 * 3) {
											this.cancel();
										}
									}

									@Override
									public synchronized void cancel() {
										mActiveRunnables.remove(this);
										super.cancel();
									}
								};
								runnable.runTaskTimer(mPlugin, 5, 1);
								mActiveRunnables.add(runnable);
							}
						}

						mWrathTicks++;
					}

					@Override
					public synchronized void cancel() {
						mActiveRunnables.remove(this);
						super.cancel();
					}
				};
				runnable.runTaskTimer(mPlugin, 80, 1);
				mActiveRunnables.add(runnable);
			}

			@Override
			public int cooldownTicks() {
				return COOLDOWN;
			}
		};

		SpellManager activeSpells = new SpellManager(List.of(spell));

		List<Spell> passiveSpells = List.of(
			teleportSpell
		);

		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, Vesperidys.detectionRange);
		super.constructBoss(activeSpells, passiveSpells, Vesperidys.detectionRange, null, DELAY);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		Location centre = LocationUtils.getEntityCenter(mBoss);
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 2.0f, 0.1f);
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_DROWNED_SHOOT, SoundCategory.PLAYERS, 0.3f, 0.1f);
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 2.0f, 0.8f);

		for (double x = -0.25; x <= 0.25; x += 0.5) {
			for (double z = -0.25; z <= 0.25; z += 0.5) {
				double finalX = x;
				double finalZ = z;
				BukkitRunnable throwRunnable = new BukkitRunnable() {
					final Location mLoc = centre.clone();
					final double mGoalY = mVesperidys.mSpawnLoc.getBlockY();
					int mThrowTicks = 0;
					final double mAccelerationY = -0.1;
					final double mVelocityX = finalX;
					double mVelocityY = 1;
					final double mVelocityZ = finalZ;

					@Override
					public void run() {
						if (mVesperidys.mDefeated) {
							this.cancel();
							return;
						}

						if (mLoc.getY() < mGoalY || mThrowTicks > 5 * 20) {
							mLoc.setY(mGoalY);
							spawnClouds(mLoc);

							this.cancel();
							return;
						}

						mVelocityY += mAccelerationY;
						Vector dir = new Vector(mVelocityX, mVelocityY, mVelocityZ);
						mLoc.add(dir);

						new PartialParticle(Particle.CLOUD, mLoc, 1, 0, 0, 0)
							.spawnAsEntityActive(mBoss);

						mThrowTicks++;
					}
				};
				throwRunnable.runTaskTimer(mPlugin, 0, 1);
			}
		}
	}

	public void spawnClouds(Location loc) {
		Hitbox hitbox = new Hitbox.SphereHitbox(loc, RADIUS);
		loc.getWorld().playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 2.0f, 0.8f);
		loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.8f, 1f);
		loc.getWorld().playSound(loc, Sound.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 0.8f, 0.67f);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mCloudTicks = 0;

			@Override
			public void run() {

				if (mVesperidys.mDefeated || mCloudTicks > CLOUD_TICKS) {
					this.cancel();
					return;
				}

				if (mCloudTicks % 5 == 0) {
					new PartialParticle(Particle.FIREWORKS_SPARK, loc, 6, 2, 2, 2, 0.1).spawnAsBoss();
					new PartialParticle(Particle.CLOUD, loc, 4, 2, 2, 2, 0.05).spawnAsBoss();
					new PartialParticle(Particle.CLOUD, loc, 3, 0.1, 0.1, 0.1, 0.15).spawnAsBoss();
				}

				double theta = ((double) mCloudTicks / 40) * 2 * Math.PI;
				for (int i = 0; i < 3; i++) {
					double angle = theta + 2 * Math.PI * i / 3;

					double x = RADIUS * Math.cos(angle);
					double z = RADIUS * Math.sin(angle);

					Location pLoc = loc.clone().add(x, 0, z);
					new PartialParticle(Particle.CLOUD, pLoc, 1, 0, 0, 0, 0.05).spawnAsBoss();

				}

				for (Player player : PlayerUtils.playersInRange(loc, Vesperidys.detectionRange, true)) {
					if (hitbox.intersects(player.getBoundingBox())) {
						player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 30, 10));
						mMonuPlugin.mEffectManager.addEffect(player, "FallFragility", new PercentDamageReceived(5 * 20, DepthsParty.getAscensionScaledDamage(1, mVesperidys.mParty), EnumSet.of(DamageEvent.DamageType.FALL)));
					}
				}

				mCloudTicks++;
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
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

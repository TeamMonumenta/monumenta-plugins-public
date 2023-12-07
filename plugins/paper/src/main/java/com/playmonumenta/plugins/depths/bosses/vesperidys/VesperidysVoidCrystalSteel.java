package com.playmonumenta.plugins.depths.bosses.vesperidys;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.DevourBoss;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellDevour;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.depths.bosses.spells.vesperidys.SpellVoidCrystalTeleportPassive;
import com.playmonumenta.plugins.effects.AbilitySilence;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
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
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Shulker;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class VesperidysVoidCrystalSteel extends BossAbilityGroup {
	public static final String identityTag = "boss_vesperidysvoidcrystalsteel";

	private final Vesperidys mVesperidys;
	private final Plugin mMonuPlugin;

	private static final double DAMAGE = 60;
	private static final int DELAY = 2 * 20;
	private static final int COOLDOWN = 15 * 20;
	private static final int TRAP_DURATION = 15 * 20;
	private static final int SILENCE_DURATION = 3 * 20;

	public static double ARMOR_STAND_BLOCK_OFFSET = -1.35;
	private final PartialParticle mSpark = new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation(), 1, 0.1, 0.1, 0.1, 0);

	public static final int MENACE_SPAWN_DURATION = 10;
	public static final double MENACE_SPAWN_HEIGHT = 10;

	public static @Nullable VesperidysVoidCrystalSteel deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return construct(plugin, boss);
	}

	public static @Nullable VesperidysVoidCrystalSteel construct(Plugin plugin, LivingEntity boss) {
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
		return new VesperidysVoidCrystalSteel(plugin, boss, vesperidys);
	}

	public VesperidysVoidCrystalSteel(Plugin plugin, LivingEntity boss, Vesperidys vesperidys) {
		super(plugin, identityTag, boss);
		mMonuPlugin = plugin;
		mVesperidys = vesperidys;

		DevourBoss.Parameters p = new DevourBoss.Parameters();
		p.DAMAGE = DAMAGE;
		p.INITIAL_ANGLE = 0;
		p.FINAL_ANGLE = 360;
		p.NUM_ITERATION = 3;
		p.RADIUS_INCREMENT = 1;
		p.Y_OFFSET = -ARMOR_STAND_BLOCK_OFFSET;
		p.INDICATOR_DELAY = 5;
		p.SOUND_INITIAL = SoundsList.fromString("[(ENTITY_ARROW_HIT, 3.0, 0.5),(ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 3.0, 0.5)]");

		Spell spell = new Spell() {

			@Override
			public void run() {
				open();
				World w = mBoss.getWorld();
				w.playSound(mBoss.getLocation(), Sound.ENTITY_VINDICATOR_CELEBRATE, 1f, 1f);

				BukkitRunnable trapsRunnable = new BukkitRunnable() {
					int mTicks = 0;

					@Override
					public void run() {
						if (mTicks > 3) {
							w.playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.HOSTILE, 3, 1);
							w.playSound(mBoss.getLocation(), Sound.ITEM_CROSSBOW_LOADING_START, SoundCategory.HOSTILE, 3, 1);
							w.playSound(mBoss.getLocation(), Sound.ITEM_CROSSBOW_LOADING_START, SoundCategory.HOSTILE, 3, 1);

							BukkitRunnable volleyRunnable = new BukkitRunnable() {
								@Override
								public void run() {
									w.playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.HOSTILE, 3, 1);
									w.playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_SHOOT, SoundCategory.HOSTILE, 3, 1);
									List<Projectile> projectiles = EntityUtils.spawnVolley(mBoss, 40, 2, 360.0 / 40, Arrow.class);
									for (Projectile projectile : projectiles) {
										AbstractArrow proj = (AbstractArrow) projectile;

										proj.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
										proj.setDamage(25);

										BukkitRunnable runB = new BukkitRunnable() {

											@Override
											public void run() {
												// spawn particle
												mSpark.location(proj.getLocation()).spawnAsEnemy();

												if (proj.isInBlock() || !proj.isValid()) {
													this.cancel();
												}
											}

										};
										runB.runTaskTimer(mPlugin, 0, 1);
										mActiveRunnables.add(runB);
									}
								}
							};
							volleyRunnable.runTaskLater(mPlugin, 20);
							mActiveRunnables.add(volleyRunnable);

							this.cancel();
							return;
						}

						Location centre = LocationUtils.getEntityCenter(mBoss);
						mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_DISPENSER_LAUNCH, 1f, 1f);

						BukkitRunnable throwRunnable = new BukkitRunnable() {
							final Location mLoc = centre.clone();
							final double mGoalY = mVesperidys.mSpawnLoc.getBlockY();
							int mThrowTicks = 0;
							final double mAccelerationY = -0.1;
							final double mVelocityX = FastUtils.randomDoubleInRange(-0.25, 0.25);
							double mVelocityY = 1;
							final double mVelocityZ = FastUtils.randomDoubleInRange(-0.25, 0.25);

							@Override
							public void run() {
								if (mVesperidys.mDefeated) {
									this.cancel();
									return;
								}

								if (mLoc.getY() < mGoalY || mThrowTicks > 5 * 20) {
									mLoc.setY(mGoalY);
									spawnTraps(mLoc);

									this.cancel();
									return;
								}

								mVelocityY += mAccelerationY;
								Vector dir = new Vector(mVelocityX, mVelocityY, mVelocityZ);
								mLoc.add(dir);

								new PartialParticle(Particle.TOTEM, mLoc, 1, 0, 0, 0)
									.spawnAsEntityActive(mBoss);

								mThrowTicks++;
							}
						};
						throwRunnable.runTaskTimer(mPlugin, 0, 1);
						mActiveRunnables.add(throwRunnable);

						mTicks++;
					}
				};
				trapsRunnable.runTaskTimer(mPlugin, 20, 15);
				mActiveRunnables.add(trapsRunnable);

				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					close();
				}, COOLDOWN / 2);
			}

			private void spawnTraps(Location loc) {
				BoundingBox hitbox = BoundingBox.of(loc, 0.3, 1, 0.3);

				ArmorStand trap = mBoss.getWorld().spawn(loc.clone().add(0, ARMOR_STAND_BLOCK_OFFSET, 0), ArmorStand.class);
				trap.setVisible(false);
				trap.setGravity(false);
				trap.setMarker(true);
				trap.setCollidable(false);
				trap.getEquipment().setHelmet(new ItemStack(Material.POLISHED_BLACKSTONE_PRESSURE_PLATE));

				mBoss.getWorld().playSound(loc, Sound.ITEM_AXE_STRIP, 1f, 0.5f);

				BukkitRunnable trapRunnable = new BukkitRunnable() {
					int mTrapTicks = 0;

					@Override
					public synchronized void cancel() throws IllegalStateException {
						super.cancel();
						trap.remove();
					}

					@Override
					public void run() {
						if (mTrapTicks > TRAP_DURATION) {
							this.cancel();
							return;
						}

						for (Player player : PlayerUtils.playersInRange(loc, 5, true)) {
							if (player.getBoundingBox().overlaps(hitbox)) {
								DamageUtils.damage(mBoss, player, DamageEvent.DamageType.PROJECTILE_SKILL, DAMAGE, null, true, true, "Snare Trap");
								mMonuPlugin.mEffectManager.addEffect(player, "TrapSlowness", new PercentSpeed(20 * 2, -1, "DummySlowness"));
								player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 2, -4));
								mMonuPlugin.mEffectManager.addEffect(player, "TrapSilence", new AbilitySilence(SILENCE_DURATION));

								new PPExplosion(Particle.CLOUD, loc)
									.extra(1)
									.count(50)
									.spawnAsBoss();
								mBoss.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3f, 2f);

								SpellDevour spellDevour = new SpellDevour(mMonuPlugin, trap, p);
								spellDevour.run();
								this.cancel();
								return;
							}
						}

						if (mTrapTicks % 2 == 0) {
							double radius = 0.5;
							double angle = 2 * Math.PI * (double) mTrapTicks / 10;

							double x = radius * Math.sin(angle);
							double z = radius * Math.cos(angle);

							Location pLoc = loc.clone().add(x, 0, z);

							new PartialParticle(Particle.REDSTONE, pLoc, 1, 0, 0, 0)
								.data(new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1))
								.spawnAsEntityActive(trap);
						}

						mTrapTicks++;
					}
				};

				trapRunnable.runTaskTimer(mPlugin, 0, 1);
				mActiveRunnables.add(trapRunnable);
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
		summonMenace(mBoss.getLocation());
	}

	private void summonMenace(Location loc) {
		// Summon a ring of particles at the spawn location to signify what's happening.
		new PPCircle(Particle.FLAME, loc, 5).count(100).extra(0).spawnAsBoss();

		// Start the Animation for the Menace spawning.
		new BukkitRunnable() {
			final Location mCurrLoc = loc.clone().add(0, MENACE_SPAWN_HEIGHT, 0);
			final Location mSpawnLoc = loc.clone();
			final double mHeightDecrease = MENACE_SPAWN_HEIGHT / (double) MENACE_SPAWN_DURATION;

			int mTicks = 0;

			@Override
			public void run() {
				if (mVesperidys.mDefeated) {
					this.cancel();
					return;
				}

				// Summon two rings of particles at current location.
				new PPCircle(Particle.FLAME, mCurrLoc, 3).ringMode(true).count(45).extra(0.1).spawnAsBoss();
				new PPCircle(Particle.FLAME, mCurrLoc, 1).ringMode(true).count(27).extra(0.05).spawnAsBoss();

				// Play a sound at the current location.
				mBoss.getWorld().playSound(mCurrLoc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.HOSTILE, 1f, 1.5f);

				// Decrease the height of the location.
				mCurrLoc.subtract(0, mHeightDecrease, 0);

				if (mTicks >= MENACE_SPAWN_DURATION) {
					// Create a particle and smoke explosion at the ground.
					new PartialParticle(Particle.FLAME, mCurrLoc, 200).extra(0.25).spawnAsBoss();
					new PartialParticle(Particle.EXPLOSION_HUGE, mCurrLoc, 1).extra(0).spawnAsBoss();
					new PPCircle(Particle.CLOUD, mCurrLoc.clone().add(0, 1, 0), 0.5).countPerMeter(5)
						.rotateDelta(true).delta(1, 0, 0).ringMode(true).extra(0.1).spawnAsBoss();
					// Summon the Menace.
					Entity menace = LibraryOfSoulsIntegration.summon(mSpawnLoc, "WorthySacrifice");
					if (menace instanceof Hoglin hoglin) {
						hoglin.addScoreboardTag("DD2BossFight3");
						mMonuPlugin.mBossManager.manuallyRegisterBoss(hoglin, new VesperidysBlockPlacerBoss(mMonuPlugin, hoglin, mVesperidys));

						mBoss.getWorld().playSound(mSpawnLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 10f, 1f);
						mBoss.getWorld().playSound(mSpawnLoc, Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10f, 2f);

						// Knock players away from the impact point
						mCurrLoc.getNearbyPlayers(2)
							.forEach(hitPlayer -> MovementUtils.knockAwayRealistic(mCurrLoc, hitPlayer, 2, 0.5f, true));
					}
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
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

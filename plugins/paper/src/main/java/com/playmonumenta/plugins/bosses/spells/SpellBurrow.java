package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.bosses.BurrowBoss;
import com.playmonumenta.plugins.effects.DamageImmunity;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;


public class SpellBurrow extends Spell {
	private static final Material[] BLOCK_MATERIALS = {Material.BROWN_WOOL, Material.BROWN_CONCRETE, Material.BROWN_TERRACOTTA};

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final BurrowBoss.Parameters mParameters;

	public SpellBurrow(Plugin plugin, LivingEntity boss, BurrowBoss.Parameters parameters) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mParameters = parameters;
	}

	@Override
	public void run() {
		burrow();
	}

	private void burrow() {
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_CAMEL_DASH, SoundCategory.HOSTILE, 2f, 1f);
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_CAMEL_DASH, SoundCategory.HOSTILE, 2f, 1f);
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_HOGLIN_CONVERTED_TO_ZOMBIFIED, SoundCategory.HOSTILE, 2f, 1.5f);

		mBoss.setInvulnerable(true);
		mBoss.setVelocity(new Vector(0, 1.1, 0));

		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mBoss.isOnGround() || mT >= 40) {
					Location floorLocation = LocationUtils.fallToGround(mBoss.getLocation(), -1);
					// Kill the entity if the location falls to void
					if (floorLocation.getY() <= -1) {
						mBoss.remove();
						this.cancel();
						return;
					}

					for (int i = 0; i < 20; i++) {
						throwBlock(floorLocation, 0.5, 0.3);
					}

					List<Entity> bossAndPassengers = mBoss.getPassengers();
					bossAndPassengers.add(mBoss);
					bossAndPassengers.forEach(mob -> {
						if (mob instanceof LivingEntity livingEntity) {
							livingEntity.setInvisible(true);
							livingEntity.setGravity(false);
							livingEntity.setFireTicks(0);
							livingEntity.setAI(false);
							GlowingManager.makeGlowImmune(livingEntity, mParameters.MAX_BURROW_DURATION, GlowingManager.BOSS_SPELL_PRIORITY - 1, null, "boss_burrow");
							if (!(livingEntity instanceof Guardian)) {
								// Exception for Guardians so that lasers are still visible while burrowing
								livingEntity.setVisibleByDefault(false);
							}
						}
					});

					mWorld.playSound(floorLocation, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2f, 0.75f);
					mWorld.playSound(floorLocation, Sound.BLOCK_SUSPICIOUS_GRAVEL_BREAK, SoundCategory.HOSTILE, 2f, 0.5f);
					mWorld.playSound(floorLocation, Sound.BLOCK_SUSPICIOUS_SAND_BREAK, SoundCategory.HOSTILE, 2f, 0.5f);
					new PPParametric(Particle.ITEM_CRACK, floorLocation, (parameter, builder) -> builder.offset(FastUtils.cos(parameter * Math.PI * 2), 1, FastUtils.sin(parameter * Math.PI * 2)))
						.data(new ItemStack(Material.COARSE_DIRT))
						.directionalMode(true)
						.delta(0, 1, 0)
						.count(100)
						.extraRange(0.2, 0.6)
						.spawnAsEnemy();

					moveUnderground(floorLocation);
					this.cancel();
					return;
				}
				mT++;
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(mPlugin, 10, 1);
	}

	private void moveUnderground(Location loc) {
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			Location mLocation = new Location(mWorld, mBoss.getX(), loc.getY(), mBoss.getZ());
			final Vector mVelocity = new Vector(0, 0, 0);
			@Nullable Player mTarget;

			@Override
			public void run() {
				mTarget = EntityUtils.getNearestPlayer(mLocation, mParameters.RANGE, false);

				double currentBurrowSpeed = mParameters.BURROW_SPEED_START + (mParameters.BURROW_SPEED_END - mParameters.BURROW_SPEED_START) * ((double) mTicks / mParameters.MAX_BURROW_DURATION);
				double currentBurrowAcceleration = mParameters.BURROW_ACCELERATION_START + (mParameters.BURROW_ACCELERATION_END - mParameters.BURROW_ACCELERATION_START) * ((double) mTicks / mParameters.MAX_BURROW_DURATION);

				if (mTarget != null) {
					Vector toTarget = mTarget.getLocation().clone().subtract(mLocation).toVector().setY(0).normalize().multiply(currentBurrowAcceleration);

					// slow the velocity if it is moving away from the target
					double dot = toTarget.clone().normalize().dot(mVelocity.clone().normalize());
					if (dot < 0.8) {
						mVelocity.multiply(mParameters.BURROW_CORRECTION_MULTIPLIER);
						if (dot < 0) {
							mVelocity.multiply(mParameters.BURROW_CORRECTION_MULTIPLIER);
						}
					}

					if (!PlayerUtils.playersInRange(mLocation, mParameters.BURROW_NEARBY_ACCELERATION_RANGE, false).isEmpty() && dot > 0.8) {
						toTarget.multiply(mParameters.BURROW_NEARBY_ACCELERATION_MULTIPLIER);
					}

					mVelocity.add(toTarget).normalize().multiply(currentBurrowSpeed);
					mLocation.add(mVelocity);
					Location loc = mLocation.clone().add(0, 1, 0);

					if (loc.getBlock().getType().isSolid()) {
						loc.add(0, 1, 0);
						if (loc.getBlock().getType().isSolid()) {
							loc.add(0, 1, 0);
							if (loc.getBlock().getType().isSolid()) {
								loc = mLocation.clone().subtract(mVelocity.clone().add(mVelocity.clone().normalize().multiply(mBoss.getBoundingBox().getWidthX())));
								this.cancel();
							}
						}
					} else if (!loc.subtract(0, 1, 0).getBlock().getType().isSolid()) {
						loc.subtract(0, 1, 0);
						if (!loc.getBlock().getType().isSolid()) {
							loc.subtract(0, 1, 0);
							if (!loc.getBlock().getType().isSolid()) {
								loc = mLocation.clone().subtract(mVelocity.clone().add(mVelocity.clone().normalize().multiply(mBoss.getBoundingBox().getWidthX())));
								this.cancel();
							}
						}
					}

					Location targetXZLoc = mTarget.getLocation();
					targetXZLoc.setY(mLocation.getY());
					if (targetXZLoc.distance(mLocation) < mParameters.EMERGE_CHECK_RADIUS) {
						this.cancel();
						return;
					}

					EntityUtils.teleportStack(mBoss, loc);
					mLocation = loc;
				}

				if (mTicks % 2 == 0) {
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_CAMEL_DASH_READY, SoundCategory.HOSTILE, 1f, 0.5f);
					mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_PACKED_MUD_BREAK, SoundCategory.HOSTILE, 1f, 0.5f);
					mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.HOSTILE, 1f, 0.5f);
				}

				new PartialParticle(Particle.BLOCK_CRACK, mLocation.clone().add(0, 1.1, 0)).data(Material.PACKED_MUD.createBlockData()).count(10).delta(0.35, 0.1, 0.35).spawnAsBoss();

				if (mTarget == null || !PlayerUtils.playersInRange(mLocation, mParameters.EMERGE_CHECK_RADIUS, false).isEmpty()) {
					this.cancel();
					return;
				}

				mTicks++;
				if (mTicks > mParameters.MAX_BURROW_DURATION || mBoss.isDead()) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				emerge(mLocation);
				super.cancel();
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(mPlugin, 0, 1);
	}

	private void emerge(Location location) {
		if (mBoss.isDead()) {
			return;
		}

		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EAT, SoundCategory.HOSTILE, 1f, 0.5f);
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ZOGLIN_ANGRY, SoundCategory.HOSTILE, 1f, 0.75f);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			final Location mGroundLocation = location.clone().add(0, 1, 0);

			@Override
			public void run() {
				if (mTicks <= mParameters.EMERGE_DELAY) {
					new PPCircle(Particle.BLOCK_CRACK, mGroundLocation, mParameters.EMERGE_RADIUS).data(Material.PACKED_MUD.createBlockData()).ringMode(true).countPerMeter(1).spawnAsBoss();
				}

				if (mTicks % 2 == 0) {
					mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.HOSTILE, 1f, 0.75f);
				}

				if (mTicks == mParameters.EMERGE_DELAY) {
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1f, 0.75f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_HURT, SoundCategory.HOSTILE, 1f, 0.5f);

					List<Entity> bossAndPassengers = mBoss.getPassengers();
					bossAndPassengers.add(mBoss);
					bossAndPassengers.forEach(mob -> {
						mob.setInvisible(false);
						mob.setInvulnerable(false);
						mob.setGravity(true);
						mob.setVisibleByDefault(true);
						if (mob instanceof LivingEntity livingEntity) {
							livingEntity.setAI(true);
							((com.playmonumenta.plugins.Plugin) mPlugin).mEffectManager.addEffect(livingEntity, "BurrowFallNegation", new DamageImmunity(4 * 20, EnumSet.of(DamageEvent.DamageType.FALL)));
						}
					});

					EntityUtils.teleportStack(mBoss, mGroundLocation);
					mBoss.setVelocity(new Vector(0, 1, 0));

					for (int i = 0; i < 20; i++) {
						throwBlock(mGroundLocation, 0.7, 0.3);
					}

					new PPParametric(Particle.SMOKE_LARGE, mGroundLocation, (parameter, builder) -> builder.offset(FastUtils.cos(parameter * Math.PI * 2), 1, FastUtils.sin(parameter * Math.PI * 2)))
						.directionalMode(true)
						.count(50)
						.extra(0.3)
						.spawnAsEnemy();
					new PPCircle(Particle.ITEM_CRACK, mGroundLocation, mParameters.EMERGE_RADIUS)
						.data(new ItemStack(Material.COARSE_DIRT))
						.countPerMeter(6)
						.directionalMode(true)
						.ringMode(false)
						.delta(0.0, 1, 0)
						.extraRange(0.2, 0.4)
						.spawnAsEnemy();
					new PPCircle(Particle.BLOCK_CRACK, mGroundLocation, mParameters.EMERGE_RADIUS)
						.data(Material.COARSE_DIRT.createBlockData())
						.countPerMeter(6)
						.ringMode(false)
						.spawnAsEnemy();

					for (int i = 0; i < mParameters.SUMMON_COUNT; i++) {
						throwAdd(mGroundLocation, 0.8, 0.6);
					}

					// Does Damage
					Collection<Player> playersHit = PlayerUtils.playersInCylinder(
						mGroundLocation,
						mParameters.EMERGE_RADIUS,
						mParameters.EMERGE_VERTICAL_HITBOX
					);

					for (Player player : playersHit) {
						Location playerXZLoc = player.getLocation();
						playerXZLoc.setY(mGroundLocation.getY());
						double falloffMultiplier = (playerXZLoc.distance(mGroundLocation) <= mParameters.FALLOFF_START) ? 1 :
							mParameters.MINIMUM_FALLOFF + ((1 - mParameters.MINIMUM_FALLOFF) * (1 - ((playerXZLoc.distance(mGroundLocation) - mParameters.FALLOFF_START) / (mParameters.EMERGE_RADIUS - mParameters.FALLOFF_START))));

						BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MELEE, mParameters.DAMAGE * falloffMultiplier, mGroundLocation);
						MovementUtils.knockAway(mGroundLocation, player, mParameters.KNOCKBACK_X, mParameters.KNOCKBACK_Y);
						mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1f, 0.5f);
					}

					this.cancel();
					return;
				}

				mTicks++;
				if (mBoss.isDead()) {
					this.cancel();
				}
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(mPlugin, 0, 1);
	}

	private void throwBlock(Location origin, double vertical, double horizontal) {
		FallingBlock block = mWorld.spawn(origin, FallingBlock.class);

		// Get surrounding block materials. For immersion
		List<Material> blockMaterials = new ArrayList<>();
		for (int i = -3; i <= 3; i++) {
			for (int j = -3; j <= 3; j++) {
				for (int k = -3; k <= 3; k++) {
					Location loc = origin.clone().add(i, j, k);
					Material material = loc.getBlock().getType();
					if (!blockMaterials.contains(material) && material.isSolid()) {
						blockMaterials.add(material);
					}
				}
			}
		}

		if (blockMaterials.isEmpty()) {
			Collections.addAll(blockMaterials, BLOCK_MATERIALS);
		}

		block.setBlockState(blockMaterials.get(FastUtils.randomIntInRange(0, blockMaterials.size() - 1)).createBlockData().createBlockState());
		block.setDropItem(false);
		block.addScoreboardTag("DisableBlockPlacement");
		double theta = FastUtils.randomDoubleInRange(0, Math.PI * 2);
		double variance = FastUtils.randomDoubleInRange(0.75, 1.25);
		block.setVelocity(new Vector(horizontal * FastUtils.cos(theta), vertical, horizontal * FastUtils.sin(theta)).multiply(variance));

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (block.getY() < origin.getY()) {
					this.cancel();
				}

				mTicks++;
				if (mTicks > 60) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();

				block.remove();
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(mPlugin, 0, 1);
	}

	private void throwAdd(Location origin, double vertical, double horizontal) {
		// don't spawn in safe zones!
		if (!ZoneUtils.hasZoneProperty(origin, ZoneUtils.ZoneProperty.NO_SUMMONS)) {
			Entity spawn = mParameters.SPAWNED_MOB_POOL.spawn(origin);
			if (spawn != null) {
				summonPlugins(spawn);
				double theta = FastUtils.randomDoubleInRange(0, Math.PI * 2);
				double variance = FastUtils.randomDoubleInRange(0.85, 1.15);
				spawn.setVelocity(new Vector(horizontal * FastUtils.cos(theta), vertical, horizontal * FastUtils.sin(theta)).multiply(variance));
			}
		}
	}

	public void summonPlugins(Entity summon) {

	}

	@Override
	public int cooldownTicks() {
		return mParameters.MAX_BURROW_DURATION + mParameters.COOLDOWN;
	}

	@Override
	public boolean canRun() {
		return !mParameters.TARGETS.getTargetsList(mBoss).isEmpty();
	}
}

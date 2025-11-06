package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.PhasesManagerBoss;
import com.playmonumenta.plugins.bosses.bosses.ProjectileEntityBoss;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.SPAWNER_COUNT_METAKEY;

public class SpellProjectileEntity extends SpellBaseSeekingProjectile {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final UUID mBossUUID;
	private final ProjectileEntityBoss.Parameters mParameters;
	BossManager bossManager = BossManager.getInstance();
	EntityTargets projEntityTargets;

	private final HashSet<UUID> mProjectileEntities = new HashSet<>();

	public SpellProjectileEntity(Plugin mPlugin, LivingEntity mBoss, ProjectileEntityBoss.Parameters p) {
		super(mPlugin, mBoss, true, 1, 0,
			p.COOLDOWN, p.SPELL_DELAY, p.OFFSET_LEFT, p.OFFSET_UP, p.OFFSET_FRONT, 0, p.FIX_YAW, p.FIX_PITCH,
			1, 30, p.SPEED, p.TURN_RADIUS, p.DURATION, p.HITBOX_LENGTH, p.LINGERS, p.COLLIDES_WITH_BLOCKS,
			1, 1, false, 0,

			//spell targets
			() -> { // If the boss is the projectile, return the first applicable target.
				if (p.PREFER_TARGET) {
					return new ArrayList<>(Collections.singletonList(((Mob) mBoss).getTarget()));
				} else if (p.ENTITY.equals(LoSPool.LibraryPool.EMPTY)) {
					List<? extends LivingEntity> targets = p.TARGETS.getTargetsList(mBoss);
					targets.removeIf(target -> target instanceof Player && AbilityUtils.isStealthed((Player) target));

					return targets.isEmpty() ? new ArrayList<>() :
						new ArrayList<>(Collections.singletonList(targets.get(0)));
				}
				return p.TARGETS.getTargetsList(mBoss);
			},

			// Initiate Aesthetic
			(World world, Location loc, int ticks) -> {
				if (p.SPELL_DELAY > 0) {
					GlowingManager.startGlowing(mBoss, NamedTextColor.NAMES.valueOr(p.COLOR, NamedTextColor.RED), p.SPELL_DELAY, GlowingManager.BOSS_SPELL_PRIORITY);
				}
				p.SOUND_START.play(loc);
			},
			// Aesthetics are not used here
			(World world, Location loc, int ticks) -> {
			},
			(World world, Location loc, int ticks) -> {
			},
			(World world, @Nullable LivingEntity target, Location loc, @Nullable Location prevLoc) -> {
			}
		);

		this.mBoss = mBoss;
		this.mPlugin = mPlugin;
		mBossUUID = mBoss.getUniqueId();
		mParameters = p;
		projEntityTargets = mParameters.LINGERS ? mParameters.TARGETS.clone()
			.setLimit(new EntityTargets.Limit(EntityTargets.Limit.LIMITSENUM.ALL)) :
			mParameters.TARGETS;
	}

	// Prevent boss from spawning multiple projectile if it itself is the entity
	@Override
	public boolean canRun() {
		return !mProjectileEntities.contains(mBossUUID) && super.canRun();
	}

	@Override
	public <V extends LivingEntity> void launch(V initialTarget, Location targetLoc, boolean fixed, double fYaw, double fPitch,
	                                            double offsetX, double offsetY, double offsetZ, double offsetYaw, double offsetPitch) {
		if (!targetLoc.getWorld().equals(mBoss.getWorld())
			|| ZoneUtils.hasZoneProperty(mBoss.getLocation(), ZoneUtils.ZoneProperty.NO_SUMMONS)
			|| mProjectileEntities.contains(mBossUUID)) { // In case it tries to cast again
			return;
		}

		final int maxRange = 50;
		final boolean isSelfProjectile = mParameters.ENTITY.equals(LoSPool.LibraryPool.EMPTY);

		final Entity projEntity = isSelfProjectile ? mBoss : mParameters.ENTITY
			.spawn(mBoss.getEyeLocation().add(offsetX, offsetY + mParameters.ENTITY_OFFSET, offsetZ).setDirection(targetLoc.toVector().normalize()));

		if (projEntity == null) {
			return;
		}

		final UUID entityUUID = projEntity.getUniqueId();
		mProjectileEntities.add(entityUUID);
		projEntity.addScoreboardTag(EntityUtils.DONT_ENTER_BOATS_TAG);

		Location startingLocation = isSelfProjectile ?
			mBoss.getLocation().add(0, projEntity.getHeight() / 2.0, 0) :
			mBoss.getEyeLocation();

		// Include the original mob's metadata for spawner counting to prevent mob farming
		if (!isSelfProjectile && mBoss.hasMetadata(SPAWNER_COUNT_METAKEY)) {
			projEntity.setMetadata(SPAWNER_COUNT_METAKEY, mBoss.getMetadata(SPAWNER_COUNT_METAKEY).get(0));
		}

		// Toggle AI if it's applicable
		if (mParameters.TOGGLE_AI && !projEntity.isDead() && projEntity.isValid()
			&& projEntity instanceof LivingEntity livingEntity) {
			livingEntity.setAI(false);
		}

		mParameters.PARTICLE_LAUNCH.spawn(mBoss, startingLocation);
		mParameters.SOUND_LAUNCH.play(startingLocation);

		BukkitRunnable runnable = new BukkitRunnable() {
			final Location mLocation = startingLocation.add(VectorUtils.rotateYAxis(
				VectorUtils.rotateXAxis(new Vector(offsetX, offsetY, offsetZ), mBoss.getLocation().getPitch()),
				mBoss.getLocation().getYaw()));
			final BoundingBox mHitbox = BoundingBox.of(mLocation, mParameters.HITBOX_LENGTH / 2, mParameters.HITBOX_LENGTH / 2, mParameters.HITBOX_LENGTH / 2);

			final Vector mBaseDir = !fixed ? targetLoc.clone().subtract(mLocation).toVector().normalize() :
				VectorUtils.rotationToVector(fYaw + mBoss.getLocation().getYaw(), fPitch);

			Vector mDirection = VectorUtils.rotateTargetDirection(
				mBaseDir, offsetYaw, offsetPitch);

			int mTicks = 0;
			@Nullable
			LivingEntity target = initialTarget;

			@Override
			public void run() {
				mTicks++;

				// If lingering is enabled, try searching for a new target
				if (target == null ||
					!target.isValid() ||
					target.isDead() ||
					!target.getWorld().equals(mLocation.getWorld()) ||
					!Double.isFinite(mDirection.getX()) ||
					(target instanceof Player p && p.getGameMode() == GameMode.SPECTATOR)) {
					if (mParameters.LINGERS && projEntity instanceof LivingEntity lEntity) {
						// Get the projectile entity possible target candidates based on boss targets, omit itself
						List<? extends LivingEntity> filter = projEntityTargets.getTargetsList(lEntity);
						filter.removeIf(t -> t.equals(projEntity)
							|| (t instanceof Player p && (p.getGameMode() == GameMode.SPECTATOR)));
						target = EntityUtils.getNearestMob(mLocation, filter); // If target is null, projectile exist will continue without a target
					} else {
						this.cancel();
						return;
					}
				}

				if (target != null) {
					@SuppressWarnings("ConstantConditions")
					Vector newDirection = target == null ? mDirection : target.getEyeLocation().add(0, mParameters.AIM_OFFSET, 0).subtract(mLocation).toVector();
					if (newDirection.length() > 2 * maxRange) {
						this.cancel();
						return;
					}
					newDirection.normalize();
					if (!Double.isFinite(newDirection.getX())) {
						newDirection = mDirection;
					}

					double newAngle = Math.acos(Math.max(-1, Math.min(1, mDirection.dot(newDirection))));

					if (newAngle < mParameters.TURN_RADIUS) {
						mDirection = newDirection;
					} else {
						double halfEndpointDistance = FastUtils.sin(newAngle / 2);

						if (halfEndpointDistance != 0) {
							double scalar = (halfEndpointDistance + FastUtils.sin(mParameters.TURN_RADIUS - newAngle / 2)) / (2 * halfEndpointDistance);
							Vector newerDirection = mDirection.clone().add(newDirection.subtract(mDirection).multiply(scalar)).normalize();
							if (Double.isFinite(newerDirection.getX())) {
								mDirection = newerDirection;
							}
						}
					}
				}

				if (!Double.isFinite(mDirection.getX())) {
					mDirection = new Vector(0, 1, 0);
				}
				Vector shift = mDirection.clone().multiply(mParameters.SPEED);

				Block block = mLocation.getBlock();
				if (mParameters.COLLIDES_WITH_BLOCKS) {
					if (!block.isLiquid() && mHitbox.overlaps(block.getBoundingBox())) {
						projectileHit(null, mLocation.subtract(mDirection.multiply(0.5)), mLocation.subtract(mDirection.multiply(0.5)));
						this.cancel();
						return;
					}
				} else {
					if (mHitbox.overlaps(block.getBoundingBox())) {
						if (block.isLiquid() || !block.isPassable()) {
							shift.multiply(mParameters.SPEED_BLOCK);
						}
					}
				}
				if (target == null) {
					shift.multiply(mParameters.SPEED_LINGER);
				}
				Location prevLoc = mLocation.clone();
				mLocation.add(shift);
				mHitbox.shift(shift);
				/*
						This is where "projectile" entity is handled
						If projEntity is valid & not dead,
						AND if ccInterrupt is enabled, should it cancel the projectile?
				*/
				final boolean notInterruptedByCC = !mParameters.CC_INTERRUPT || !(projEntity instanceof LivingEntity) || !EntityUtils.shouldCancelSpells((LivingEntity) projEntity);

				if (!projEntity.isDead() && projEntity.isValid() && notInterruptedByCC) {
					// Center if projEntity is the boss
					double yOffset = mParameters.ENTITY_OFFSET - (isSelfProjectile ? mBoss.getHeight() / 2.0 : 0);
					EntityUtils.teleportStack(projEntity, mLocation.clone().add(0, yOffset, 0));

					mParameters.PARTICLE_PROJECTILE.spawn(mBoss, mLocation, 0.1, 0.1, 0.1, 0.1);
					if (mTicks % 40 == 0) {
						mParameters.SOUND_PROJECTILE.play(mLocation);
					}
					// Is projectile facing applicable to this mob?
					if (mParameters.FACE) {
						double[] yawPitch = VectorUtils.vectorToRotation(mLocation.clone().subtract(prevLoc).toVector().normalize());
						projEntity.setRotation((float) yawPitch[0], (float) yawPitch[1]);
					}
				} else {
					this.cancel();
					return;
				}

				Collection<LivingEntity> entities = EntityUtils
					.getNearbyMobs(mLocation, mParameters.HITBOX_LENGTH + 2, mParameters.HITBOX_LENGTH + 2, mParameters.HITBOX_LENGTH + 2,
						e -> mHitbox.overlaps(e.getBoundingBox()));
				for (LivingEntity entity : entities) {
					// If it can collide with a player, check if it's the initial target or if it can hit others
					// Otherwise, check if collision is the target
					boolean shouldHit = entity instanceof Player player?
						mParameters.COLLIDES_WITH_PLAYERS && player.getGameMode() != GameMode.SPECTATOR && (player.equals(target) || mParameters.COLLIDE_OTHER_PLAYERS) :
						entity.equals(target);

					if (shouldHit) {
						projectileHit(entity, mLocation, prevLoc);
						this.cancel();
						return;
					}
				}

				if (mTicks > mParameters.DURATION) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() {
				projectileEnd(entityUUID);
				if (!mParameters.LINGERS) {
					mActiveRunnables.remove(this);
				}
				super.cancel();
			}
		};

		runnable.runTaskTimer(mPlugin, 1, 1);
		if (!mParameters.LINGERS) {
			mActiveRunnables.add(runnable);
		}
	}

	public void projectileHit(@Nullable LivingEntity target, Location loc, Location prevLoc) {
		mParameters.SOUND_HIT.play(loc, 0.5f, 0.5f);
		mParameters.PARTICLE_HIT.spawn(mBoss, loc, 0d, 0d, 0d, 0.25d);
		if (target != null) {
			if (mParameters.DAMAGE > 0) {
				BossUtils.blockableDamage(mBoss, target, mParameters.DAMAGE_TYPE, mParameters.DAMAGE,
					mParameters.SPELL_NAME, prevLoc, mParameters.EFFECTS.mEffectList());
				mParameters.EFFECTS.apply(target, mBoss);
			}
			if (mParameters.TRUE_DAMAGE_PERCENTAGE > 0) {
				DamageUtils.damagePercentHealth(mBoss, target, mParameters.TRUE_DAMAGE_PERCENTAGE, false, mParameters.TRUE_DAMAGE_BLOCK, mParameters.SPELL_NAME, true, List.of());
			}
		}
	}

	public void projectileEnd(UUID uuid) {
		Entity boss = Bukkit.getEntity(uuid);
		mProjectileEntities.remove(uuid);
		if (boss == null || !boss.isValid()) {
			return;
		}
		if (!mParameters.CUSTOM.isEmpty()) {
			PhasesManagerBoss phasesManagerBoss = bossManager.getBoss(boss, PhasesManagerBoss.class);
			if (phasesManagerBoss != null) {
				phasesManagerBoss.onCustomTrigger(mParameters.CUSTOM);
			}
		}
		switch (mParameters.TYPE) {
			case KILL -> {
				if (boss instanceof LivingEntity entity && !boss.isDead()) {
					// Don't want to give kill credit for a mob that dies as a projectile
					boss.addScoreboardTag(EntityUtils.IGNORE_DEATH_TRIGGERS_TAG);
					entity.setHealth(0.0);
					return;
				}
				boss.remove(); // Incase the above fails
			}
			case PERSIST -> {
				if (boss instanceof LivingEntity entity && !boss.isDead() && mParameters.TOGGLE_AI) {
					entity.setAI(true);
				}
			}
			case REMOVE -> boss.remove();
		}
	}

	@Override
	public void onDeath(@Nullable EntityDeathEvent event) {
		if (!mParameters.LINGERS) {
			mProjectileEntities.forEach(this::projectileEnd);
		}
	}

}

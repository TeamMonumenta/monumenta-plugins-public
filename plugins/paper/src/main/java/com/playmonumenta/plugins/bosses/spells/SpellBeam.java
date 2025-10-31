package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.bosses.BeamBoss;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.List;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class SpellBeam extends Spell {
	protected final Plugin mPlugin;
	protected final LivingEntity mBoss;
	protected final BeamBoss.Parameters mParameters;

	public SpellBeam(Plugin plugin, LivingEntity entity, BeamBoss.Parameters parameters) {
		mPlugin = plugin;
		mBoss = entity;
		mParameters = parameters;
	}

	@Override
	public void run() {
		if (mParameters.ROOT_DURATION > 0) {
			EntityUtils.selfRoot(mBoss, mParameters.ROOT_DURATION);
		}
		List<? extends LivingEntity> targets;
		if (mParameters.PREFER_TARGET) {
			LivingEntity target = ((Mob) mBoss).getTarget();
			targets = (target != null && target.getLocation().distance(mBoss.getLocation()) < mParameters.TARGETS.getRange())
				? List.of(target)
				: List.of();
		} else {
			targets = mParameters.TARGETS.getTargetsList(mBoss);
		}
		if (targets.isEmpty()) {
			return;
		}
		targets.stream()
			.filter(target -> !(target instanceof Player && AbilityUtils.isStealthed((Player) target)))
			.forEach(this::castTelegraphBeam);
		mParameters.SOUND_TELE.play(mBoss.getLocation());
	}

	// Check the "max range" of the beam
	public double beamCheckDistance(Vector direction) {
		if (!mParameters.STOP_AT_BLOCK && !mParameters.STOP_AT_PLAYER) {
			return mParameters.BEAM_RANGE;
		}
		Location center = retrieveOffset();
		RayTraceResult trace = mBoss.getWorld().rayTrace(
			center,
			direction,
			mParameters.BEAM_RANGE,
			FluidCollisionMode.NEVER,
			true,
			0.5, // We want an accurate distance, so no hitbox size here
			(Entity e) -> !e.equals(mBoss) && mParameters.STOP_AT_PLAYER && e instanceof Player,
			(Block b) -> mParameters.STOP_AT_BLOCK && b.isSolid()
		);
		if (trace == null) {
			return mParameters.BEAM_RANGE;
		}

		double blockDist = trace.getHitBlock() != null
			? center.distance(trace.getHitPosition().toLocation(mBoss.getWorld()))
			: mParameters.BEAM_RANGE;

		double entityDist = trace.getHitEntity() != null
			? center.distance(trace.getHitEntity().getLocation())
			: mParameters.BEAM_RANGE;
		return Math.min(blockDist, entityDist);
	}

	public Vector getTargetVector(LivingEntity target) {
		if (mParameters.FACE_TARGET) {
			return mBoss.getLocation().getDirection().normalize();
		}
		Location bossLoc = retrieveOffset();
		Location targetLoc;
		if (!mParameters.LOCK_PITCH && target.getLocation().getY() >= bossLoc.getY()) {
			targetLoc = target.getLocation();
		} else if (!mParameters.LOCK_PITCH && target.getLocation().getY() + target.getHeight() <= bossLoc.getY()) {
			targetLoc = target.getLocation().add(0, target.getHeight(), 0);
		} else {
			targetLoc = target.getLocation();
			targetLoc.setY(bossLoc.getY());
		}
		return targetLoc.subtract(bossLoc).toVector().normalize();
	}

	public void castTelegraphBeam(LivingEntity target) {
		if (EntityUtils.shouldCancelSpells(mBoss)) {
			return;
		}
		double[] yawRotation = new double[mParameters.SPLIT];
		for (int i = 0; i < mParameters.SPLIT; i++) {
			yawRotation[i] = Math.toRadians(mParameters.SPLIT_ANGLE * (i - (mParameters.SPLIT - 1) / 2.0));
			int j = i;
			BukkitRunnable runnable = new BukkitRunnable() {
				int mTicks = 0;
				Vector mDirection = getTargetVector(target).rotateAroundY(yawRotation[j]);

				@Override
				public void run() {
					if (mTicks < mParameters.SPELL_DELAY) {
						if (mTicks < mParameters.BEAM_TRACK && isValidTracking(target)) {
							mDirection = getTargetVector(target).rotateAroundY(yawRotation[j]);
						}
						if (mTicks % mParameters.TELEGRAPH_INTERVAL == 0) {
							Location start = retrieveOffset();
							Location end = start.clone().add(mDirection.clone()
								.multiply(beamCheckDistance(mDirection))
							);
							// Particle Group: Telegraph
							for (ParticlesList.CParticle p : mParameters.PARTICLE_TELE.getParticleList()) {
								new PPLine(p.mParticle, start, end)
									.delta(p.mDx, p.mDy, p.mDz)
									.countPerMeter(p.mCount)
									.extra(p.mVelocity)
									.data(p.mExtra2)
									.spawnAsEntityActive(mBoss);
							}
						}
					} else {
						// Particle Group: Boss
						for (ParticlesList.CParticle p : mParameters.PARTICLE_BOSS.getParticleList()) {
							new PartialParticle(p.mParticle, retrieveOffset())
								.count(p.mCount)
								.delta(p.mDx, p.mDy, p.mDz)
								.extra(p.mVelocity)
								.data(p.mExtra2)
								.spawnAsEntityActive(mBoss);
						}
						castBeam(mDirection);
						this.cancel();
						return;
					}
					mTicks++;
					if (mTicks > mParameters.SPELL_DELAY || mBoss.isDead() || EntityUtils.shouldCancelSpells(mBoss)) {
						this.cancel();
					}
				}
			};
			mActiveRunnables.add(runnable);
			runnable.runTaskTimer(mPlugin, 0, 1);
		}
	}

	public void castBeam(Vector direction) {
		if (EntityUtils.shouldCancelSpells(mBoss)) {
			return;
		}
		Location center = retrieveOffset();
		double beamRange = beamCheckDistance(direction);
		Vector beamRangeVector = direction.clone().normalize().multiply(beamRange);

		final List<Player> struckPlayers =
			Hitbox.approximateCylinder(center, center.clone().add(beamRangeVector), mParameters.HITBOX_SIZE, false)
				.getHitPlayers(true);

		for (Player player : struckPlayers) {
			mParameters.SOUND_HIT.play(player.getLocation());
			if (mParameters.DAMAGE > 0) {
				BossUtils.blockableDamage(mBoss, player, mParameters.DAMAGE_TYPE, mParameters.DAMAGE, !mParameters.RESPECT_IFRAMES, false, mParameters.NAME, mBoss.getLocation(), mParameters.EFFECTS.mEffectList());
			}
			if (mParameters.TRUE_DAMAGE_PERCENTAGE > 0) {
				DamageUtils.damagePercentHealth(mBoss, player, mParameters.TRUE_DAMAGE_PERCENTAGE, false, false, mParameters.NAME, true, mParameters.EFFECTS.mEffectList());
			}
			if (mParameters.KB_Y != 0 && mParameters.KB_XZ != 0) {
				MovementUtils.knockAway(mBoss.getLocation(), player, mParameters.KB_XZ, mParameters.KB_Y, true);
			}
			if (mParameters.STOP_AT_PLAYER) {
				break;
			}
		}
		mParameters.SOUND_CAST.play(mBoss.getLocation());

		Vector axis1 = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize().multiply(0.3);
		Vector axis2 = direction.clone().crossProduct(axis1).normalize().multiply(0.3);
		// Particle Group: Helix
		for (ParticlesList.CParticle p : mParameters.PARTICLE_HELIX.getParticleList()) {
			new PPParametric(p.mParticle, center, (parameter, builder) -> {
				double distance = parameter * beamRange;
				double theta = (distance < 7 ? (1.9 * Math.pow(0.2 * distance, 3)) : 1.6 * distance - 6) % (Math.PI * 2);
				Location point = center.clone().add(direction.clone().multiply(distance))
					.add(axis1.clone().multiply(FastUtils.cos(theta) * mParameters.HELIX_SIZE))
					.add(axis2.clone().multiply(FastUtils.sin(theta) * mParameters.HELIX_SIZE));
				builder.location(point);
			})
				.count(p.mCount * ((int) Math.ceil(beamRange)))
				.delta(p.mDx, p.mDy, p.mDz)
				.extra(p.mVelocity)
				.data(p.mExtra2)
				.spawnAsEntityActive(mBoss);
		}
		// Particle Group: Beam
		for (ParticlesList.CParticle p : mParameters.PARTICLE_BEAM.getParticleList()) {
			new PPLine(p.mParticle, center, center.clone().add(direction.clone().multiply(beamRange)))
				.countPerMeter(p.mCount)
				.delta(p.mDx, p.mDy, p.mDz)
				.extra(p.mVelocity)
				.data(p.mExtra2)
				.spawnAsEntityActive(mBoss);
		}
		// Particle Group: Circle
		Location circleCenter = center.clone().add(direction.clone().multiply(2));
		for (ParticlesList.CParticle p : mParameters.PARTICLE_CIRCLE.getParticleList()) {
			new PPParametric(p.mParticle, circleCenter, (parameter, builder) -> {
				double theta = parameter * Math.PI * 2;
				builder.location(circleCenter.clone()
					.add(axis1.clone().multiply(FastUtils.cos(theta) * mParameters.HELIX_SIZE))
					.add(axis2.clone().multiply(FastUtils.sin(theta) * mParameters.HELIX_SIZE)));
				Vector offset = axis1.clone().multiply(FastUtils.cos(theta + Math.PI / 2))
					.add(axis2.clone().multiply(FastUtils.sin(theta + Math.PI / 2)));
				builder.offset(offset.getX(), offset.getY(), offset.getZ());
			})
				.directionalMode(true)
				.count(p.mCount)
				.delta(p.mDx, p.mDy, p.mDz)
				.extra(p.mVelocity)
				.data(p.mExtra2)
				.spawnAsEntityActive(mBoss);
		}
	}


	private Location retrieveOffset() {
		return LocationUtils.getHalfHeightLocation(mBoss).add(0, mParameters.Y_OFFSET, 0);
	}

	private boolean isValidTracking(LivingEntity target) {
		if (target == null) {
			return false;
		}
		return target.isValid() && !target.isDead() && target.getWorld().equals(mBoss.getWorld())
			&& !(target instanceof Player player && player.getGameMode().equals(GameMode.SPECTATOR));
	}

	@Override
	public int cooldownTicks() {
		return mParameters.COOLDOWN;
	}
}

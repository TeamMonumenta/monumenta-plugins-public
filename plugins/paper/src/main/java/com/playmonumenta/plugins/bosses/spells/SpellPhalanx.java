package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.bosses.PhalanxBoss;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellPhalanx extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final PhalanxBoss.Parameters mParameters;

	public SpellPhalanx(Plugin plugin, LivingEntity boss, PhalanxBoss.Parameters parameters) {
		mPlugin = plugin;
		mBoss = boss;
		mParameters = parameters;
	}

	@Override
	public void run() {
		for (LivingEntity target : mParameters.TARGETS.getTargetsList(mBoss)) {
			createPhalanx(target);
		}
	}

	@Override
	public int cooldownTicks() {
		return mParameters.COOLDOWN;
	}


	private void createPhalanx(LivingEntity target) {
		mParameters.SOUND_PHALANX.play(mBoss.getLocation());

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				Location loc = LocationUtils.getHalfHeightLocation(mBoss).clone();
				loc.setDirection(LocationUtils.getDirectionTo(target.getLocation(), mBoss.getLocation()).setY(0).normalize());
				Vector vec;
				boolean playersInTriggerRadius = !PlayerUtils.playersInRange(mBoss.getLocation(), mParameters.TRIGGER_RADIUS, true).isEmpty();

				double degree = mParameters.START_ANGLE;
				int degreeSteps = mParameters.PROJ_COUNT - 1;
				double degreeStep = mParameters.SPLIT_ANGLE;
				for (int step = 0; step <= degreeSteps; step++, degree += degreeStep) {
					double radian1 = FastMath.toRadians(degree);
					vec = new Vector(FastUtils.cos(radian1) * mParameters.PHALANX_RADIUS, FastUtils.sin(radian1) * mParameters.PHALANX_RADIUS, 0);
					vec = VectorUtils.rotateXAxis(vec, loc.clone().getPitch());
					vec = VectorUtils.rotateYAxis(vec, loc.clone().getYaw());

					Location l = loc.clone().add(0, 0, 0).add(vec);
					mParameters.PARTICLES_PHALANX.spawn(mBoss, l);
					if (mTicks > mParameters.PHALANX_DURATION_MIN && playersInTriggerRadius) {
						launchProjectile(target, l);
						this.cancel();
					}
				}

				if (mTicks > mParameters.PHALANX_DURATION_MAX) {
					this.cancel();
				}

				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void launchProjectile(LivingEntity target, Location l) {
		Vector dirToPlayer = target.getLocation().clone().toVector().add(new Vector(0, 1.5, 0)).subtract(l.clone().toVector()).normalize();
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			final Location mLocation = l;
			final Vector mToPlayer = dirToPlayer.clone().multiply(mParameters.PROJECTILE_SPEED * FastUtils.randomDoubleInRange(0.8, 1.2));

			@Override
			public void run() {
				if (mTicks == 0) {
					mParameters.SOUND_SHOOT.play(l);
				}

				// move the projectile
				Vector angleDelta = mToPlayer.clone().crossProduct(mToPlayer.clone().crossProduct(target.getLocation().clone().toVector().add(new Vector(0, 1.5, 0)).subtract(mLocation.clone().toVector())));
				if (!angleDelta.isZero()) {
					angleDelta.normalize().multiply(mParameters.TURN_SPEED);
				}
				mToPlayer.add(angleDelta.multiply(-1)).normalize().multiply(mParameters.PROJECTILE_SPEED);
				mLocation.add(mToPlayer);

				// check hitboxes
				Hitbox hitbox = new Hitbox.AABBHitbox(mBoss.getWorld(), BoundingBox.of(mLocation, 0.25, 0.25, 0.25));
				if (hitbox.getHitPlayers(true).contains(target)) {
					mParameters.PARTICLE_HIT.spawn(mBoss, LocationUtils.getHalfHeightLocation(target));
					mParameters.SOUND_HIT.play(target.getLocation());

					DamageUtils.damage(mBoss, target, DamageEvent.DamageType.PROJECTILE, mParameters.DAMAGE, null, true, true, mParameters.SPELL_NAME);

					this.cancel();
				}

				mParameters.PARTICLE_PROJECTILE.spawn(mBoss, mLocation);

				mTicks++;
				if (mTicks > mParameters.MAX_LIFETIME || mBoss.isDead()) {
					this.cancel();
				}
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(mPlugin, 0, 1);
	}

}

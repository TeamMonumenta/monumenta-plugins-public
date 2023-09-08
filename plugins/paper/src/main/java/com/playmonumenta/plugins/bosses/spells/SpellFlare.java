package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.bosses.FlareBoss;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.AbstractMap;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellFlare extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mLauncher;
	private final FlareBoss.Parameters mParameters;

	public SpellFlare(Plugin plugin, LivingEntity launcher, FlareBoss.Parameters parameters) {
		mPlugin = plugin;
		mLauncher = launcher;
		mParameters = parameters;
	}

	@Override
	public void run() {
		if (!mParameters.CAN_MOVE) {
			EntityUtils.selfRoot(mLauncher, mParameters.FUSE_TIME);
		}
		for (LivingEntity target : mParameters.TARGETS.getTargetsList(mLauncher)) {
			mParameters.SOUND_WARNING.play(target.getLocation());
			performFlare(target);
		}
	}

	@Override
	public int cooldownTicks() {
		return mParameters.COOLDOWN;
	}

	protected void performFlare(LivingEntity target) {
		if (target == null) {
			return;
		}

		new BukkitRunnable() {
			int mTicks = 0;
			final Location mTargetLocation = target.getLocation().clone();
			final Location mLauncherLocation = mLauncher.getLocation();


			@Override
			public void run() {
				if (mLauncher.isDead() || !mLauncher.isValid() || EntityUtils.isStunned(mLauncher) || EntityUtils.isSilenced(mLauncher)) {
					this.cancel();
					return;
				}
				if (mTicks <= 0) {
					performDirectionalTel(mTargetLocation, mLauncherLocation, mParameters.RANGE);
				}
				mTicks++;
				chargeActions(mLauncherLocation, mTicks);
				if (mTicks >= mParameters.FUSE_TIME) {
					this.cancel();
					performExplosion(mTargetLocation, mLauncherLocation, mParameters.RANGE);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	protected void chargeActions(Location locLauncher, int ticks) {
		if (ticks % 20 == 0) {
			mParameters.SOUND_CHARGE_TARGET.play(locLauncher);
			for (int i = 0; i < 360; i += 18) {
				mParameters.PARTICLES_CHARGE_TWENTY_TICKS_BORDER.spawn(mLauncher, locLauncher.clone().add(FastUtils.cos(Math.toRadians(i)) * mParameters.RADIUS, 0, FastUtils.sin(Math.toRadians(i)) * mParameters.RADIUS));
			}
		}
		mParameters.SOUND_CHARGE_BOSS.play(mLauncher.getLocation());
		mParameters.PARTICLES_CHARGE_BOSS.spawn(mLauncher, mLauncher.getLocation().clone().add(0, mLauncher.getHeight() / 2, 0));

		if (ticks == mParameters.FUSE_TIME) {
			mParameters.SOUND_EXPLOSION.play(locLauncher);
			ParticleUtils.explodingRingEffect(com.playmonumenta.plugins.Plugin.getInstance(), locLauncher.clone().add(0, 0.2, 0), mParameters.RADIUS, 0, 3,
				List.of(
					new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(1.0, (Location location) -> mParameters.PARTICLES_EXPLOSION_RINGS.spawn(mLauncher, location))
				)
			);
			for (int i = 0; i < 5; i++) {
				Location hLoc = locLauncher.clone().add(0, i * 0.85, 0);
				mParameters.PARTICLES_PILLAR.spawn(mLauncher, hLoc);
			}
			for (Player p : PlayerUtils.playersInRange(locLauncher, mParameters.RADIUS, true)) {
				mParameters.SOUND_EXPLOSION_PLAYER.play(p.getLocation());
				DamageUtils.damage(mLauncher, p, DamageType.BLAST, mParameters.DAMAGE);
				if (mParameters.DO_KNOCK_UP) {
					double knockupSpeed = mParameters.KNOCK_UP_SPEED + (p.getLocation().distance(locLauncher) <= mParameters.RADIUS / 2.0 ? 0.5 : 0);
					p.setVelocity(p.getVelocity().add(new Vector(0.0, knockupSpeed, 0.0)));
				}
			}
		}
	}


	protected void performExplosion(Location loc, Location locLauncher, double range) {
		Vector targetDirection = LocationUtils.getDirectionTo(loc, locLauncher).setY(0).normalize();
		new BukkitRunnable() {
			final Location mLoc = locLauncher;
			double mStep = 0;
			int mTimes = 0;

			@Override
			public void run() {
				if (mTimes > 0) {
					if (mStep == 0) {
						mLoc.setDirection(targetDirection);
					}

					Vector vec;
					mStep += (int) (range / (mParameters.EXPLOSIONS - 1));
					double degree = mParameters.START_ANGLE;
					int degreeSteps = mParameters.SPLITS - 1;
					double degreeStep = mParameters.SPLIT_ANGLE;

					for (int step = 0; step <= degreeSteps; step++, degree += degreeStep) {
						double radian1 = FastMath.toRadians(degree);
						vec = new Vector(FastUtils.cos(radian1) * mStep, 0.2, FastUtils.sin(radian1) * mStep);
						vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch());
						vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

						Location l = mLoc.clone().add(0, 0, 0).add(vec);
						mParameters.SOUND_EXPLOSION.play(l);
						ParticleUtils.explodingRingEffect(com.playmonumenta.plugins.Plugin.getInstance(), l, mParameters.RADIUS, 0, 3,
							List.of(
								new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(1.0, (Location location) -> mParameters.PARTICLES_EXPLOSION_RINGS.spawn(mLauncher, location))
							)
						);

						for (int i = 0; i < 5; i++) {
							Location hLoc = l.clone().add(0, i * 0.85, 0);
							mParameters.PARTICLES_PILLAR.spawn(mLauncher, hLoc);
						}

						// Damage and knock up players
						for (Player p : PlayerUtils.playersInRange(l, mParameters.RADIUS, true)) {
							mParameters.SOUND_EXPLOSION_PLAYER.play(p.getLocation());
							DamageUtils.damage(mLauncher, p, DamageType.BLAST, mParameters.DAMAGE);
							if (mParameters.DO_KNOCK_UP) {
								double knockupSpeed = mParameters.KNOCK_UP_SPEED + (p.getLocation().distance(l) <= mParameters.RADIUS / 2.0 ? 0.5 : 0);
								p.setVelocity(p.getVelocity().add(new Vector(0.0, knockupSpeed, 0.0)));
							}
						}
					}

					if (mStep >= range - 1) {
						this.cancel();
					}
				}
				mTimes++;
			}

		}.runTaskTimer(com.playmonumenta.plugins.Plugin.getInstance(), 0, mParameters.EXPLOSION_INTERVAL);
	}

	protected void performDirectionalTel(Location loc, Location locLauncher, double range) {
		Vector targetDirection = LocationUtils.getDirectionTo(loc, locLauncher).setY(0).normalize();
		new BukkitRunnable() {
			final Location mLoc = locLauncher;
			double mStep = 0;
			@Override
			public void run() {

				if (mStep == 0) {
					mLoc.setDirection(targetDirection);
				}

				Vector vec;
				mStep += mParameters.TEL_SPEED;
				double degree = mParameters.START_ANGLE;
				int degreeSteps = mParameters.SPLITS - 1;
				double degreeStep = mParameters.SPLIT_ANGLE;

				for (int step = 0; step <= degreeSteps; step++, degree += degreeStep) {
					double radian1 = FastMath.toRadians(degree);
					vec = new Vector(FastUtils.cos(radian1) * mStep, 0.125, FastUtils.sin(radian1) * mStep);
					vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch());
					vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

					Location l = mLoc.clone().add(0, 0.1, 0).add(vec);
					mParameters.PARTICLES_FLARE_TEL.spawn(mLauncher, l);
				}

				if (mStep >= range - 1) {
					this.cancel();
				}
			}
		}.runTaskTimer(com.playmonumenta.plugins.Plugin.getInstance(), 0, 1);
	}
}

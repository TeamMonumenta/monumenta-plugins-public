package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.bosses.LacerateBoss;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class SpellLacerate extends Spell {

	public final Plugin mPlugin;
	public final LivingEntity mLauncher;
	public final LacerateBoss.Parameters mParameters;

	public SpellLacerate(Plugin plugin, LivingEntity launcher, LacerateBoss.Parameters parameters) {
		mPlugin = plugin;
		mLauncher = launcher;
		mParameters = parameters;
	}

	@Override
	public void run() {
		for (LivingEntity target : mParameters.TARGETS.getTargetsList(mLauncher)) {
			mParameters.SOUND_WARNING.play(target.getLocation());
			for (ParticlesList.CParticle particle : mParameters.TELEGRAPH_BEAM.getParticleList()) {
				new PPLine(particle.mParticle, mLauncher.getEyeLocation(), target.getLocation())
					.delta(0.4, 0.4, 0.4)
					.data(particle.mExtra2)
					.countPerMeter(20)
					.spawnAsEntityActive(mLauncher);
			}
			performFlurry(target);
		}
	}

	@Override
	public int cooldownTicks() {
		return mParameters.COOLDOWN;
	}

	protected void performFlurry(LivingEntity target) {
		if (target == null) {
			return;
		}

		new BukkitRunnable() {
			int mTicks = 0;
			final Location mTargetLocation = target.getLocation().clone();

			@Override
			public void run() {
				if (mLauncher.isDead() || !mLauncher.isValid() || EntityUtils.isStunned(mLauncher) || EntityUtils.isSilenced(mLauncher)) {
					this.cancel();
					return;
				}

				mTicks++;
				chargeActions(mTargetLocation, mTicks);
				if (mTicks >= mParameters.TELEGRAPH_DURATION) {
					this.cancel();
					performExplosion(mTargetLocation);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	protected void chargeActions(Location loc, int ticks) {
		if (ticks <= (mParameters.TELEGRAPH_DURATION - 5)) {
			mParameters.SOUND_CHARGE_BOSS.play(loc);
			if (ticks % 2 == 0) {
				for (ParticlesList.CParticle particle : mParameters.TELEGRAPH_CIRCLE.getParticleList()) {
					new PPCircle(particle.mParticle, loc, mParameters.RADIUS)
						.count(20)
						.randomizeAngle(true)
						.data(particle.mExtra2)
						.delta(0.02, 0.08, 0.02)
						.spawnAsEntityActive(mLauncher);
				}
			}
		}
	}


	protected void performExplosion(Location loc) {
		mParameters.SOUND_EXPLOSION.play(loc);
		final Location finalLoc = loc.add(0, 1, 0);

		BukkitRunnable doFlurry = new BukkitRunnable() {
			int mInc = 0;
			float mFlurryPitchIncrease = 0;

			@Override
			public void run() {
				mInc++;
				if (mParameters.SHOW_LINES) {
					for (int i = 0; i < 6; i++) {
						createRandomLine(finalLoc, mLauncher);
					}
				}

				if (mInc <= mParameters.HIT_AMOUNT) {
					mFlurryPitchIncrease += 0.125f;
					mParameters.SOUND_FLURRY_INCREMENT.playSoundsModified(cSound -> cSound.setPitch(cSound.getPitch() + mFlurryPitchIncrease), loc);
					mParameters.SOUND_FLURRY_BACKGROUND.play(loc);
					for (Player p : PlayerUtils.playersInRange(loc, mParameters.RADIUS, true)) {
						new PartialParticle(mParameters.ON_HIT_PARTICLE, p.getLocation(), 5, 0.1, 0.1, 0.1, 0.75).spawnAsEntityActive(mLauncher);
						MovementUtils.knockAwayRealistic(mLauncher.getLocation(), p, 0.1f, 0.03f, false);
						DamageUtils.damage(mLauncher, p, mParameters.DAMAGE_TYPE, mParameters.DAMAGE, null, true);
					}
				} else {
					for (Player p : PlayerUtils.playersInRange(loc, mParameters.RADIUS, true)) {
						new PartialParticle(mParameters.FINISHER_PARTICLE, p.getLocation(), 3, 0.1, 0.1, 0.1, 0.75).spawnAsEntityActive(mLauncher);
						if (!mParameters.RESPECT_IFRAMES || p.getNoDamageTicks() == 0) {
							DamageUtils.damage(mLauncher, p, mParameters.DAMAGE_TYPE, mParameters.FINISHER_DAMAGE, null, !mParameters.RESPECT_IFRAMES);
							MovementUtils.knockAwayRealistic(mLauncher.getLocation(), p, (float) mParameters.KNOCK_AWAY, (float) mParameters.KNOCK_UP, false);
						}
					}

					if (mParameters.SHOW_SLASHES) {
						Location launcherLoc = mLauncher.getEyeLocation();
						Vector targetDirection = loc.toVector().subtract(launcherLoc.toVector()).normalize();
						Location outerSlashDirection = loc.clone().setDirection(targetDirection);
						ParticleUtils.drawCleaveArc(outerSlashDirection, mParameters.RADIUS * 0.8, 160, -80, 260, mParameters.RINGS, 0, 0, mParameters.SPACING, 60,
							(Location l, int ring) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
								new Particle.DustOptions(
									ParticleUtils.getTransition(mParameters.SLASH_COLOR_INNER, mParameters.SLASH_COLOR_OUTER, ring / 8D),
									(mParameters.FORCED_PARTICLE_SIZE > 0) ? (float) mParameters.FORCED_PARTICLE_SIZE : 0.6f + (ring * 0.1f)
								)).spawnAsEntityActive(mLauncher));

						ParticleUtils.drawCleaveArc(outerSlashDirection, mParameters.RADIUS * 0.8, 20, -80, 260, mParameters.RINGS, 0, 0, mParameters.SPACING, 60,
							(Location l, int ring) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
								new Particle.DustOptions(
									ParticleUtils.getTransition(mParameters.SLASH_COLOR_INNER, mParameters.SLASH_COLOR_OUTER, ring / 8D),
									(mParameters.FORCED_PARTICLE_SIZE > 0) ? (float) mParameters.FORCED_PARTICLE_SIZE : 0.6f + (ring * 0.1f)
								)).spawnAsEntityActive(mLauncher));
					}
					if (mParameters.SHOW_END_LINE) {
						Location pLoc = loc.add(0, mParameters.RADIUS * 0.7, 0);
						new PartialParticle(mParameters.END_LINE_PARTICLE, pLoc, 35, 0, 0, 0, 0.15).spawnAsEntityActive(mLauncher);
						pLoc.setPitch(0);
						ParticleUtils.drawParticleLineSlash(pLoc, VectorUtils.rotateTargetDirection(pLoc.getDirection(), 90, 15),
							0, mParameters.RADIUS, 0.05, 5,
							(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
								float size = (float) (0.75f + (0.4f * middleProgress));
								new PartialParticle(Particle.REDSTONE, lineLoc, 3, 0.05, 0.05, 0.05, 0.25,
									new Particle.DustOptions(mParameters.END_LINE_COLOR, size)).spawnAsEntityActive(mLauncher);
							});

						ParticleUtils.drawParticleLineSlash(pLoc, VectorUtils.rotateTargetDirection(pLoc.getDirection(), 90, 15),
							0, mParameters.RADIUS, 0.2, 5,
							(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> new PartialParticle(mParameters.END_LINE_PARTICLE, lineLoc, 2, 0, 0, 0, 0).spawnAsEntityActive(mLauncher));
					}

					mParameters.SOUND_FINISHER.play(loc);
					this.cancel();
				}
			}
		};
		doFlurry.runTaskTimer(com.playmonumenta.plugins.Plugin.getInstance(), 0, mParameters.LINE_INTERVAL);
	}

	private void createRandomLine(Location loc, LivingEntity mLauncher) {
		final double flurryLineLength = mParameters.LINE_LENGTH;
		loc = loc.clone().add(
			FastUtils.randomDoubleInRange(-mParameters.RADIUS * 0.8, mParameters.RADIUS * 0.8),
			FastUtils.randomDoubleInRange(-mParameters.RADIUS * 0.8, mParameters.RADIUS * 0.8),
			FastUtils.randomDoubleInRange(-mParameters.RADIUS * 0.8, mParameters.RADIUS * 0.8)
		);

		Vector dir = new Vector(
			FastUtils.randomDoubleInRange(-1, 1),
			FastUtils.randomDoubleInRange(-0.75, 0.75),
			FastUtils.randomDoubleInRange(-1, 1)
		).normalize();

		loc.setDirection(dir);

		ParticleUtils.drawParticleLineSlash(loc, dir, 0, flurryLineLength, 0.05, 5,
			(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
				float size = (float) (0.3f + (0.35f * middleProgress));
				new PartialParticle(Particle.REDSTONE, lineLoc, mParameters.LINE_PARTICLE_COUNT,
					0.05, 0.05, 0.05, 0.25,
					new Particle.DustOptions(mParameters.LINE_COLOR, size)).spawnAsEntityActive(mLauncher);
				if (middle) {
					if (mParameters.SHOW_EXPLOSIONS) {
						ParticleUtils.drawParticleCircleExplosion(mLauncher, lineLoc.clone().setDirection(dir), 0,
							mParameters.RADIUS * 0.25, 0, 90,
							mParameters.EXPLOSION_POINTS, mParameters.EXPLOSION_SPEED,
							true, 0, 0, mParameters.EXPLOSION_PARTICLES);
					}
				}

			});
	}
}

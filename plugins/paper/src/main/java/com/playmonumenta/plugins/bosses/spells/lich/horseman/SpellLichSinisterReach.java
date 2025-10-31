package com.playmonumenta.plugins.bosses.spells.lich.horseman;

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

/*
 * Sinister Reach - The horseman pauses momentarily for 0.8 seconds, afterwards the swing forward
targeting the player who has his aggro using the shadows to extend his reach. Each player in a
60 degree cone in front of them 8 blocks in length takes 20/32 damage, given slowness 3 and rooted
 for 5 seconds.
 */
public class SpellLichSinisterReach extends Spell {
	private static final String SPELL_NAME = "Sinister Reach";
	private static final String SLOWNESS_SRC = "SinisterReachSlowness";

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mLoc;
	private final double mRange;

	public SpellLichSinisterReach(Plugin plugin, LivingEntity entity, Location loc, double range) {
		mPlugin = plugin;
		mBoss = entity;
		mLoc = loc;
		mRange = range;
	}

	@Override
	public void run() {
		LivingEntity horse;
		LivingEntity target = null;

		mBoss.setAI(false);
		if (mBoss.getVehicle() != null) {
			horse = (LivingEntity) mBoss.getVehicle();
			horse.setAI(false);
		}
		if (mBoss instanceof Creature creature) {
			target = creature.getTarget();
		}

		if (target == null) {
			List<Player> players = Lich.playersInRange(mLoc, mRange, true);
			Collections.shuffle(players);
			if (!players.isEmpty()) {
				target = players.get(0);
			}
		}

		if (target != null) {
			World world = mBoss.getWorld();
			LivingEntity tar = target;
			double degree = 20.0 / 2.0;
			world.playSound(mBoss.getLocation(), Sound.ENTITY_CAT_HISS, SoundCategory.HOSTILE, 3, 0.5f);
			world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 3, 0.85f);
			world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 3, 1.25f);
			BukkitRunnable runA = new BukkitRunnable() {
				final Vector mDir = LocationUtils.getDirectionTo(tar.getLocation().add(0, 1, 0), mBoss.getLocation());
				final Location mTLoc = mBoss.getLocation().setDirection(mDir);
				int mT = 0;

				@Override
				public void run() {
					LivingEntity horse = null;
					if (mBoss.getVehicle() != null) {
						horse = (LivingEntity) mBoss.getVehicle();
						horse.setVelocity(new Vector(0, 0, 0));
					}

					if (mBoss.isDead() || !mBoss.isValid()) {
						this.cancel();
						return;
					}

					mT++;

					Vector vec;
					for (double r = 1; r < 5; r += 0.5) {
						for (double m = degree * -1; m <= degree; m += 5) {
							double radian1 = Math.toRadians(m);
							float yaw = mTLoc.getYaw();
							float yaw1 = yaw + 90;
							vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
							vec = VectorUtils.rotateYAxis(vec, yaw1);
							vec = VectorUtils.rotateXAxis(vec, mTLoc.getPitch());

							Location l = mTLoc.clone().add(vec);
							new PartialParticle(Particle.CRIT, l, 1, 0.5, 0.5, 0.5, 0.05).spawnAsEntityActive(mBoss);
						}
					}

					if (mT >= 30) {
						this.cancel();
						mBoss.setAI(true);
						if (horse != null) {
							horse.setAI(true);
						}
						world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 3, 0.9f);
						world.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 3, 0.75f);
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.HOSTILE, 3, 0.75f);
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 3, 1.65f);

						for (Player player : Lich.playersInRange(mBoss.getLocation(), 4, true)) {
							if (mLoc.distance(player.getLocation()) < mRange) {
								Vector toVector = player.getLocation().toVector().subtract(mBoss.getLocation().toVector()).normalize();
								if (mDir.dot(toVector) > 0.06) {
									multiHit(player);
								}
							}
						}
						for (double r = 1; r < 5; r += 0.5) {
							for (double n = degree * -1; n <= degree; n += 5) {
								double radian1 = Math.toRadians(n);
								float yaw = mTLoc.getYaw();
								float yaw2 = yaw + 90;
								vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
								vec = VectorUtils.rotateXAxis(vec, mTLoc.getPitch());
								vec = VectorUtils.rotateYAxis(vec, yaw2);
								Location l = mTLoc.clone().add(vec);
								new PartialParticle(Particle.FLAME, l, 1, 0.1, 0.1, 0.1, 0.065).spawnAsEntityActive(mBoss);
								if (r >= 4.5) {
									new PartialParticle(Particle.SWEEP_ATTACK, l, 1, 0.1, 0.1, 0.1, 0).spawnAsEntityActive(mBoss);
								}
							}
						}
						this.cancel();
					}
				}

			};
			runA.runTaskTimer(mPlugin, 0, 1);
			mActiveRunnables.add(runA);
		}
	}

	public void multiHit(Player p) {
		BukkitRunnable runB = new BukkitRunnable() {
			int mInc = 0;

			@Override
			public void run() {
				mBoss.setAI(false);
				LivingEntity horse = null;
				if (mBoss.getVehicle() != null) {
					horse = (LivingEntity) mBoss.getVehicle();
					horse.setAI(false);
				}
				mInc++;

				if (mInc < 24 && mInc % 2 == 0) {
					// Doesn't matter if the player is blocking, there are 12 hits and only one can be blocked
					DamageUtils.damage(mBoss, p, new DamageEvent.Metadata(DamageEvent.DamageType.TRUE, null,
						null, SPELL_NAME), 1, true, true, true);
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p, SLOWNESS_SRC,
						new PercentSpeed(TICKS_PER_SECOND, -1.0, SLOWNESS_SRC));
					new PartialParticle(Particle.CRIT_MAGIC, p.getLocation()).count(15).delta(0.1).extra(0.75).spawnAsEntityActive(mBoss);
				}
				if (mInc >= 24) {
					mBoss.setAI(true);
					if (horse != null) {
						horse.setAI(true);
					}
					this.cancel();
				}
			}
		};
		runB.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runB);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 10;
	}

}

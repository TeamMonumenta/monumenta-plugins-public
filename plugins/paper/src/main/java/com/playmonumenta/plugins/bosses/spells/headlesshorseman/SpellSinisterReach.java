package com.playmonumenta.plugins.bosses.spells.headlesshorseman;

import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.bosses.HeadlessHorsemanBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

/*
 * Sinister Reach - The horseman pauses momentarily for 0.8 seconds, afterwards the swing forward
targeting the player who has his aggro using the shadows to extend his reach. Each player in a
60 degree cone in front of them 8 blocks in length takes 20/32 damage, given slowness 3 and rooted
 for 5 seconds.
 */
public class SpellSinisterReach extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private HeadlessHorsemanBoss mHorseman;

	public SpellSinisterReach(Plugin plugin, LivingEntity entity, HeadlessHorsemanBoss horseman) {
		mPlugin = plugin;
		mBoss = entity;
		mHorseman = horseman;
	}

	@Override
	public void run() {
		LivingEntity target = null;
		mBoss.setAI(false);
		LivingEntity horse = null;
		if (mBoss.getVehicle() != null) {
			horse = (LivingEntity) mBoss.getVehicle();
			horse.setAI(false);
		}
		if (mBoss instanceof Creature) {
			Creature c = (Creature) mBoss;
			target = c.getTarget();
		}

		if (target == null) {
			List<Player> players = PlayerUtils.playersInRange(mHorseman.getSpawnLocation(), HeadlessHorsemanBoss.detectionRange);
			Collections.shuffle(players);
			if (players.size() > 0) {
				target = players.get(0);
			}
		}

		if (target != null) {
			World world = mBoss.getWorld();
			LivingEntity tar = target;
			double degree = 20 / 2;
			world.playSound(mBoss.getLocation(), Sound.ENTITY_CAT_HISS, 3, 0.5f);
			world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 3, 0.85f);
			world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 3, 1.25f);
			new BukkitRunnable() {
				int mT = 0;
				Vector mDir = LocationUtils.getDirectionTo(tar.getLocation().add(0, 1, 0), mBoss.getLocation());
				Location mTloc = mBoss.getLocation().setDirection(mDir);

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
							float yaw = mTloc.getYaw();
							float yaw1 = yaw + 90;
							vec = new Vector(Math.cos(radian1) * r, 0, Math.sin(radian1) * r);
							vec = VectorUtils.rotateYAxis(vec, yaw1);
							vec = VectorUtils.rotateXAxis(vec, -mTloc.getPitch());

							Location l = mTloc.clone().add(vec);
							world.spawnParticle(Particle.CRIT, l, 1, 0.5, 0.5, 0.5, 0.05);
						}
					}

					if (mT >= 30) {
						this.cancel();
						mBoss.setAI(true);
						horse.setAI(true);
						world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 3, 0.9f);
						world.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 3, 0.75f);
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 3, 0.75f);
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 3, 1.65f);

						for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 4)) {
							if (mHorseman.getSpawnLocation().distance(player.getLocation()) < HeadlessHorsemanBoss.detectionRange) {
								Vector toVector = player.getLocation().toVector().subtract(mBoss.getLocation().toVector()).normalize();
								if (mDir.dot(toVector) > 0.06) {
									multiHit(player);
								}
							}
						}
						for (double r = 1; r < 5; r += 0.5) {
							for (double n = degree * -1; n <= degree; n += 5) {
								double radian1 = Math.toRadians(n);
								float yaw = mTloc.getYaw();
								float yaw2 = yaw + 90;
								vec = new Vector(Math.cos(radian1) * r, 0, Math.sin(radian1) * r);
								vec = VectorUtils.rotateXAxis(vec, -mTloc.getPitch());
								vec = VectorUtils.rotateYAxis(vec, yaw2);
								Location l = mTloc.clone().add(vec);
								world.spawnParticle(Particle.FLAME, l, 2, 0.1, 0.1, 0.1, 0.065);
								if (r >= 4.5) {
									world.spawnParticle(Particle.SWEEP_ATTACK, l, 1, 0.1, 0.1, 0.1, 0);
								}
							}
						}
						this.cancel();
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	public void multiHit(Player p) {
		new BukkitRunnable() {
			int mNDT = p.getNoDamageTicks();
			int mInc = 0;
			@Override
			public void run() {
				World world = mBoss.getWorld();
				mBoss.setAI(false);
				LivingEntity horse = null;
				if (mBoss.getVehicle() != null) {
					horse = (LivingEntity) mBoss.getVehicle();
					horse.setAI(false);
				}
				mInc++;

				if (mInc < 36 && mInc % 2 == 0) {
					p.setNoDamageTicks(0);
					world.spawnParticle(Particle.CRIT_MAGIC, p.getLocation(), 30, 0.1, 0.1, 0.1, 0.75);
					BossUtils.bossDamagePercent(mBoss, p, 1.0, mBoss.getLocation(), true);
					// Doesn't matter if the player is blocking, there are 18 hits and only one can be blocked
					p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 10));
				}
				if (mInc >= 36) {
					p.setNoDamageTicks(mNDT);
					mBoss.setAI(true);
					horse.setAI(true);
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int duration() {
		// TODO Auto-generated method stub
		return 20 * 5;
	}

}

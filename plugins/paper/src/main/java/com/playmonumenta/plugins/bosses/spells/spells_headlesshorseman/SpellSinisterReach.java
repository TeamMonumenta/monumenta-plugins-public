package com.playmonumenta.plugins.bosses.spells.spells_headlesshorseman;

import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Horse;
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
import com.playmonumenta.plugins.utils.MovementUtils;
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
		if (mBoss.getVehicle() != null) {
			if (mBoss.getVehicle() instanceof Horse) {
				Horse horse = (Horse) mBoss.getVehicle();
				horse.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 16, 100));
			}
		}

		LivingEntity target = null;
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
			world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 3, 0.85f);
			world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 3, 1.25f);
			new BukkitRunnable() {
				int t = 0;
				Vector dir = LocationUtils.getDirectionTo(tar.getLocation().add(0, 1, 0), mBoss.getLocation());
				Location tloc = mBoss.getLocation().setDirection(dir);
				@Override
				public void run() {
					t++;

					Vector vec;
					for (double r = 1; r < 10; r += 0.5) {
						for (double degree = 60; degree < 121; degree += 8) {
							double radian1 = Math.toRadians(degree);
							vec = new Vector(Math.cos(radian1) * r, 0, Math.sin(radian1) * r);
							vec = VectorUtils.rotateXAxis(vec, -tloc.getPitch());
							vec = VectorUtils.rotateYAxis(vec, tloc.getYaw());

							Location l = tloc.clone().add(vec);
							world.spawnParticle(Particle.SMOKE_NORMAL, l, 1, 0.1, 0.1, 0.1, 0.05);
						}
					}

					if (t >= 20) {
						this.cancel();
						world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 3, 0.9f);
						world.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 3, 0.75f);
						world.playSound(mBoss.getLocation(), Sound.ITEM_TRIDENT_THROW, 3, 0.75f);
						world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 3, 1.65f);

						for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 8)) {
							if (mHorseman.getSpawnLocation().distance(player.getLocation()) < HeadlessHorsemanBoss.detectionRange) {
								Vector toVector = player.getLocation().toVector().subtract(mBoss.getLocation().toVector()).normalize();
								if (dir.dot(toVector) > .33) {
									BossUtils.bossDamage(mBoss, player, 32, (event) -> {
										if (!event.isPlayerBlocking()) {
											player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 5, 3));
											player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 5, -4));
										} else {
											MovementUtils.knockAway(mBoss.getLocation(), player, .6f, .6f);
										}
									});
								}
							}
						}
						new BukkitRunnable() {
							double degree = 60;
							@Override
							public void run() {

								Vector vec;
								for (double r = 1; r < 8; r += 0.5) {

									for (double d = degree; d < degree + 30; d += 8) {
										double radian1 = Math.toRadians(d);
										vec = new Vector(Math.cos(radian1) * r, 0, Math.sin(radian1) * r);
										vec = VectorUtils.rotateXAxis(vec, -tloc.getPitch());
										vec = VectorUtils.rotateYAxis(vec, tloc.getYaw());

										Location l = tloc.clone().add(vec);
										world.spawnParticle(Particle.FLAME, l, 2, 0.1, 0.1, 0.1, 0.065);
										if (r >= 7.5) {
											world.spawnParticle(Particle.SWEEP_ATTACK, l, 1, 0.1, 0.1, 0.1, 0);
										}
									}
								}

								degree += 30;
								if (degree >= 150) {
									this.cancel();
								}
							}

						}.runTaskTimer(mPlugin, 0, 1);
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public int duration() {
		// TODO Auto-generated method stub
		return 20 * 5;
	}

}

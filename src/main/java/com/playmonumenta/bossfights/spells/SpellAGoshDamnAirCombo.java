package com.playmonumenta.bossfights.spells;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.bossfights.utils.Utils;


public class SpellAGoshDamnAirCombo extends SpellBaseCharge {

	public SpellAGoshDamnAirCombo(Plugin plugin, LivingEntity boss, int range, int chargeTicks) {
		super(plugin, boss, range, chargeTicks,

		// Warning sound/particles at boss location and slow boss
		(Player player) -> {
			boss.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, boss.getLocation(), 50, 0.45, 0.45, 0.45, 0.15);
			boss.getWorld().spawnParticle(Particle.SWEEP_ATTACK, boss.getLocation(), 50, 0.45, 0.45, 0.45, 0.15);
			boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4), true);
			boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0f);
		},
		// Warning particles
		(Location loc) -> {
			loc.getWorld().spawnParticle(Particle.SPELL_INSTANT, loc, 2, 0.5, 0.5, 0.5, 0);
		},
		// Charge attack sound/particles at boss location
		(Player player) -> {
			boss.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, boss.getLocation(), 50, 0.45, 0.45, 0.45, 0.15);
			boss.getWorld().spawnParticle(Particle.SWEEP_ATTACK, boss.getLocation(), 50, 0.45, 0.45, 0.45, 0.15);
			boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.65f);
			boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.85f);
		},
		// Attack hit a player
		(Player player) -> {
			new BukkitRunnable() {

				@Override
				public void run() {
					player.getWorld().spawnParticle(Particle.LAVA, player.getLocation(), 80, 1.25, 0.1, 1.25, 0);
					player.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, player.getLocation(), 50, 1.25, 0.1, 1.25, 0);
					boss.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.85f, 1f);
					player.damage(12, boss);
					player.setVelocity(new Vector(0, 1.15, 0));
					boss.setVelocity(new Vector(0, 1.23, 0));
					boss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 3, 10));
					new BukkitRunnable() {

						@Override
						public void run() {
							player.damage(12, boss);
							boss.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.3F, 1);
							boss.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.3F, 0);
							Location loc = player.getLocation().add(0, 1, 0);
							player.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 50, 0, 0, 0, 0.25);
							player.getWorld().spawnParticle(Particle.FLAME, loc, 150, 0, 0, 0, 0.175);
							player.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0, 0, 0, 0.25);
							player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 75, 3, 3, 3, 0);
							Utils.KnockAway(boss.getLocation(), player, 0.65f);
							boss.setVelocity(new Vector(0, -5, 0));
							new BukkitRunnable() {
								int t = 0;
								@Override
								public void run() {
									t++;

									if (t >= 20 || boss.isOnGround()) {
										this.cancel();
										boss.setFallDistance(0);
										boss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
									}

								}

							}.runTaskTimer(plugin, 0, 1);

							// Prevent the player from taking fall damage
							player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 6, 2));
							player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 6, 1));
							player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 3, 10));
							new BukkitRunnable() {
								int t = 0;
								@Override
								public void run() {
									t++;

									if (t >= 20 || player.isOnGround()) {
										this.cancel();
										player.setFallDistance(0);
										player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
									}

								}

							}.runTaskTimer(plugin, 0, 1);
						}

					}.runTaskLater(plugin, 14);
				}

			}.runTaskLater(plugin, 1);
		},
		// Attack particles
		(Location loc) -> {
			loc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 7, 0.5, 0.5, 0.5, 0.125);
		},
		// Ending particles on boss
		() -> {
			boss.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, boss.getLocation(), 50, 0.45, 0.45, 0.45, 0.15);
			boss.getWorld().spawnParticle(Particle.SWEEP_ATTACK, boss.getLocation(), 50, 0.45, 0.45, 0.45, 0.15);
		}, true);

	}

}

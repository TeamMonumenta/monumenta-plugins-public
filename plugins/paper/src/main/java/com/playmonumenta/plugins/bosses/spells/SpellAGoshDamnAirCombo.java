package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class SpellAGoshDamnAirCombo extends SpellBaseCharge {

	public SpellAGoshDamnAirCombo(Plugin plugin, LivingEntity boss, int range, int chargeTicks) {
		super(plugin, boss, range, 160, chargeTicks, true,

			// Warning sound/particles at boss location and slow boss
			(LivingEntity player) -> {
				new PartialParticle(Particle.EXPLOSION_NORMAL, boss.getLocation(), 50, 0.45, 0.45, 0.45, 0.15).spawnAsEntityActive(boss);
				new PartialParticle(Particle.SWEEP_ATTACK, boss.getLocation(), 50, 0.45, 0.45, 0.45, 0.15).spawnAsEntityActive(boss);
				boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4));
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1.5f, 0f);
			},
			// Warning particles
			(Location loc) -> new PartialParticle(Particle.SPELL_INSTANT, loc, 2, 0.5, 0.5, 0.5, 0).spawnAsEntityActive(boss),
			// Charge attack sound/particles at boss location
			(LivingEntity player) -> {
				new PartialParticle(Particle.EXPLOSION_NORMAL, boss.getLocation(), 50, 0.45, 0.45, 0.45, 0.15).spawnAsEntityActive(boss);
				new PartialParticle(Particle.SWEEP_ATTACK, boss.getLocation(), 50, 0.45, 0.45, 0.45, 0.15).spawnAsEntityActive(boss);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1f, 1.65f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1f, 0.85f);
			},
			// Attack hit a player
			(LivingEntity target) -> {
				if (target instanceof Player player) {
					Bukkit.getScheduler().runTaskLater(plugin, () -> {
						new PartialParticle(Particle.LAVA, player.getLocation(), 80, 1.25, 0.1, 1.25, 0).spawnAsEntityActive(boss);
						new PartialParticle(Particle.EXPLOSION_NORMAL, player.getLocation(), 50, 1.25, 0.1, 1.25, 0).spawnAsEntityActive(boss);
						boss.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.85f, 1f);
						BossUtils.blockableDamage(boss, player, DamageType.MELEE, 12);
						player.setVelocity(new Vector(0, 1.15, 0));
						boss.setVelocity(new Vector(0, 1.23, 0));
						boss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 3, 10));

						Bukkit.getScheduler().runTaskLater(plugin, () -> {
							BossUtils.blockableDamage(boss, player, DamageType.MELEE, 12);
							boss.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.3F, 1);
							boss.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1.3F, 0);
							Location loc = player.getLocation().add(0, 1, 0);
							new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 50, 0, 0, 0, 0.25).spawnAsEntityActive(boss);
							new PartialParticle(Particle.FLAME, loc, 150, 0, 0, 0, 0.175).spawnAsEntityActive(boss);
							new PartialParticle(Particle.SMOKE_LARGE, loc, 50, 0, 0, 0, 0.25).spawnAsEntityActive(boss);
							new PartialParticle(Particle.SWEEP_ATTACK, loc, 75, 3, 3, 3, 0).spawnAsEntityActive(boss);
							MovementUtils.knockAway(boss.getLocation(), player, 0.65f, false);
							boss.setVelocity(new Vector(0, -5, 0));
							new BukkitRunnable() {
								int mTicks = 0;

								@Override
								public void run() {
									mTicks++;

									if (mTicks >= 20 || boss.isOnGround()) {
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
								int mTicks = 0;

								@Override
								public void run() {
									mTicks++;

									if (mTicks >= 20 || PlayerUtils.isOnGround(player)) {
										this.cancel();
										player.setFallDistance(0);
										player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
									}

								}

							}.runTaskTimer(plugin, 0, 1);
						}, 14);
					}, 1);
				}
			},
			// Attack particles
			(Location loc) -> new PartialParticle(Particle.SMOKE_NORMAL, loc, 7, 0.5, 0.5, 0.5, 0.125).spawnAsEntityActive(boss),
			// Ending particles on boss
			() -> {
				new PartialParticle(Particle.EXPLOSION_NORMAL, boss.getLocation(), 50, 0.45, 0.45, 0.45, 0.15).spawnAsEntityActive(boss);
				new PartialParticle(Particle.SWEEP_ATTACK, boss.getLocation(), 50, 0.45, 0.45, 0.45, 0.15).spawnAsEntityActive(boss);
			});

	}

}

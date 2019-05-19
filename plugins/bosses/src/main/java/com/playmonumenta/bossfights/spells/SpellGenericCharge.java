package com.playmonumenta.bossfights.spells;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;

public class SpellGenericCharge extends SpellBaseCharge {
	public SpellGenericCharge(Plugin plugin, LivingEntity boss, int range, float damage) {
		super(plugin, boss, range, 25, false,
		      // Warning sound/particles at boss location and slow boss
		      (Player player) -> {
		          boss.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, boss.getLocation(), 50, 2, 2, 2, 0);
		          boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4), true);
		          boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 1.5f);
		      },
		      // Warning particles
		      (Location loc) -> {
		          loc.getWorld().spawnParticle(Particle.CRIT, loc, 2, 0.65, 0.65, 0.65, 0);
		      },
		      // Charge attack sound/particles at boss location
		      (Player player) -> {
		          boss.getWorld().spawnParticle(Particle.SMOKE_LARGE, boss.getLocation(), 125, 0.3, 0.3, 0.3, 0.15);
		          boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.5f);
		      },
		      // Attack hit a player
		      (Player player) -> {
		          player.getWorld().spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 5, 0.4, 0.4, 0.4, 0.4, Material.REDSTONE_BLOCK.createBlockData());
		          player.getWorld().spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 12, 0.4, 0.4, 0.4, 0.4, Material.REDSTONE_WIRE.createBlockData());
		          player.damage(damage, boss);
		      },
		      // Attack particles
		      (Location loc) -> {
		          loc.getWorld().spawnParticle(Particle.FLAME, loc, 4, 0.5, 0.5, 0.5, 0.075);
		          loc.getWorld().spawnParticle(Particle.CRIT, loc, 8, 0.5, 0.5, 0.5, 0.75);
		      },
		      // Ending particles on boss
		      () -> {
		          boss.getWorld().spawnParticle(Particle.SMOKE_LARGE, boss.getLocation(), 125, 0.3, 0.3, 0.3, 0.15);
		          boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.5f);
		      });
	}
}

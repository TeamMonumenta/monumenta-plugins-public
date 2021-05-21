package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.utils.BossUtils;

public class SpellGenericCharge extends SpellBaseCharge {
	public SpellGenericCharge(Plugin plugin, LivingEntity boss, int range, float damage) {
		this(plugin, boss, range, damage, 160, 25, false, Particle.VILLAGER_ANGRY, Particle.CRIT, Particle.SMOKE_LARGE, Particle.FLAME, Particle.SMOKE_LARGE);
	}

	public SpellGenericCharge(Plugin plugin, LivingEntity boss, int range, float damage, int cooldown, int duration, boolean stopOnFirstHit,
		Particle start, Particle warning, Particle charge, Particle attack, Particle end) {
		super(plugin, boss, range, cooldown, duration, stopOnFirstHit,
		      // Warning sound/particles at boss location and slow boss
		      (Player player) -> {
		          boss.getWorld().spawnParticle(start, boss.getLocation(), 50, 2, 2, 2, 0);
		          boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 1.5f);
		          boss.setAI(false);
		      },
		      // Warning particles
		      (Location loc) -> {
		          loc.getWorld().spawnParticle(warning, loc, 2, 0.65, 0.65, 0.65, 0);
		      },
		      // Charge attack sound/particles at boss location
		      (Player player) -> {
		          boss.getWorld().spawnParticle(charge, boss.getLocation(), 125, 0.3, 0.3, 0.3, 0.15);
		          boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.5f);
		      },
		      // Attack hit a player
		      (Player player) -> {
		          player.getWorld().spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 5, 0.4, 0.4, 0.4, 0.4, Material.REDSTONE_BLOCK.createBlockData());
		          player.getWorld().spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 12, 0.4, 0.4, 0.4, 0.4, Material.REDSTONE_WIRE.createBlockData());
		          BossUtils.bossDamage(boss, player, damage);
		      },
		      // Attack particles
		      (Location loc) -> {
		          loc.getWorld().spawnParticle(attack, loc, 4, 0.5, 0.5, 0.5, 0.075);
		          loc.getWorld().spawnParticle(warning, loc, 8, 0.5, 0.5, 0.5, 0.75);
		      },
		      // Ending particles on boss
		      () -> {
		          boss.getWorld().spawnParticle(end, boss.getLocation(), 125, 0.3, 0.3, 0.3, 0.15);
		          boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.5f);
		          boss.setAI(true);
		      });
	}
}

package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class SpellGenericCharge extends SpellBaseCharge {
	public SpellGenericCharge(Plugin plugin, LivingEntity boss, int range, float damage) {
		this(plugin, boss, range, damage, 160, 25, false, Particle.VILLAGER_ANGRY, Particle.CRIT, Particle.SMOKE_LARGE, Particle.FLAME, Particle.SMOKE_LARGE);
	}

	public SpellGenericCharge(Plugin plugin, LivingEntity boss, int range, float damage, int cooldown, int duration, boolean stopOnFirstHit,
	                          Particle start, Particle warning, Particle charge, Particle attack, Particle end) {
		super(plugin, boss, range, cooldown, duration, stopOnFirstHit,
			// Warning sound/particles at boss location and slow boss
			(LivingEntity player) -> {
				new PartialParticle(start, boss.getLocation(), 50, 2, 2, 2, 0).spawnAsEntityActive(boss);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1f, 1.5f);
				boss.setAI(false);
			},
			// Warning particles
			(Location loc) -> {
				new PartialParticle(warning, loc, 2, 0.65, 0.65, 0.65, 0).spawnAsEntityActive(boss);
			},
			// Charge attack sound/particles at boss location
			(LivingEntity player) -> {
				new PartialParticle(charge, boss.getLocation(), 125, 0.3, 0.3, 0.3, 0.15).spawnAsEntityActive(boss);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1f, 1.5f);
			},
			// Attack hit a player
			(LivingEntity player) -> {
				new PartialParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 5, 0.4, 0.4, 0.4, 0.4, Material.REDSTONE_BLOCK.createBlockData()).spawnAsEntityActive(boss);
				new PartialParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 12, 0.4, 0.4, 0.4, 0.4, Material.REDSTONE_WIRE.createBlockData()).spawnAsEntityActive(boss);
				BossUtils.blockableDamage(boss, player, DamageType.MELEE, damage);
			},
			// Attack particles
			(Location loc) -> {
				new PartialParticle(attack, loc, 4, 0.5, 0.5, 0.5, 0.075).spawnAsEntityActive(boss);
				new PartialParticle(warning, loc, 8, 0.5, 0.5, 0.5, 0.75).spawnAsEntityActive(boss);
			},
			// Ending particles on boss
			() -> {
				new PartialParticle(end, boss.getLocation(), 125, 0.3, 0.3, 0.3, 0.15).spawnAsEntityActive(boss);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1f, 1.5f);
				boss.setAI(true);
			});
	}
}

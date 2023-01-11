package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpellTsunamiCharge extends SpellBaseCharge {
	public SpellTsunamiCharge(Plugin plugin, LivingEntity boss, int range, float damage) {
		super(plugin, boss, range, 25,
			// Warning sound/particles at boss location and slow boss
			(LivingEntity player) -> {
				new PartialParticle(Particle.VILLAGER_ANGRY, boss.getLocation(), 50, 2, 2, 2, 0).spawnAsEntityActive(boss);
				boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4));
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1f, 1.5f);
			},
			// Warning particles
			(Location loc) -> {
				new PartialParticle(Particle.DRIP_WATER, loc, 1, 1, 1, 1, 0).spawnAsEntityActive(boss);
			},
			// Charge attack sound/particles at boss location
			(LivingEntity player) -> {
				new PartialParticle(Particle.CLOUD, boss.getLocation(), 100, 2, 2, 2, 0).spawnAsEntityActive(boss);
				boss.getWorld().playSound(boss.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.HOSTILE, 1f, 1.5f);
			},
			// Attack hit a player
			(LivingEntity player) -> {
				new PartialParticle(Particle.DAMAGE_INDICATOR, player.getLocation(), 80, 1, 1, 1, 0).spawnAsEntityActive(boss);
				BossUtils.blockableDamage(boss, player, DamageType.MAGIC, damage);
			},
			// Attack particles
			(Location loc) -> {
				new PartialParticle(Particle.WATER_WAKE, loc, 1, 0.02, 0.02, 0.02, 0).spawnAsEntityActive(boss);
			},
			// Ending particles on boss
			() -> {
				new PartialParticle(Particle.CLOUD, boss.getLocation(), 200, 2, 2, 2, 0).spawnAsEntityActive(boss);
			});
	}
}

package com.playmonumenta.bossfights.spells;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class SpellMobHealAoE extends SpellBaseAoE {

	public SpellMobHealAoE(Plugin plugin, Entity launcher) {
		super(plugin, launcher, 14, 80, 20 * 7, false, Sound.ITEM_TRIDENT_RETURN, 0.8f, 2,
			(Location loc) -> {
				World world = loc.getWorld();
				world.spawnParticle(Particle.SPELL_INSTANT, loc, 25, 3.5, 3.5, 3.5);
			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.spawnParticle(Particle.SPELL_INSTANT, loc, 2, 0.25, 0.25, 0.25, 0, null, true);
			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 3, 1.25f);
				world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 3, 2f);
				world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 125, 0, 0, 0, 0.5, null, true);
				world.spawnParticle(Particle.VILLAGER_HAPPY, loc, 125, 3.5, 3.5, 3.5, 0.5, null, true);
			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.spawnParticle(Particle.CRIT_MAGIC, loc, 5, 0.25, 0.25, 0.25, 0.35, null, true);
				world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 4, 0.25, 0.25, 0.25, 0.15, null, true);
			},
			(Location loc) -> {
				for (Entity e : loc.getWorld().getNearbyEntities(loc, 14, 7, 14)) {
					if (e instanceof LivingEntity && EntityUtils.isHostileMob(e)) {
						LivingEntity le = (LivingEntity) e;
						double hp = le.getHealth() + 25;
						double max = le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
						if (hp >= max) {
							int missing = (int) (hp - max);
							le.setHealth(max);
							PotionUtils.applyPotion(le, le, new PotionEffect(PotionEffectType.ABSORPTION, 60 * 20, missing / 4, true, false));
						} else {
							le.setHealth(hp);
						}
						World world = loc.getWorld();
						world.spawnParticle(Particle.FIREWORKS_SPARK, le.getLocation().add(0, 1, 0), 25, 0.25, 0.5, 0.25, 0.3);
						world.spawnParticle(Particle.HEART, le.getLocation().add(0, 1, 0), 5, 0.4, 0.5, 0.4);
					}
				}
			}
		);
	}


}

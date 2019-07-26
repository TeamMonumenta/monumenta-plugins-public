package com.playmonumenta.bossfights.spells;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SpellMobHealAoE extends SpellBaseAoE {

	public SpellMobHealAoE(Plugin plugin, Entity launcher) {
		super(plugin, launcher, 7, 80, 20 * 7, false, Sound.ITEM_TRIDENT_RETURN, 0.8f, 2,
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
				for (Entity e : loc.getWorld().getNearbyEntities(loc, 7, 7, 7)) {
					if (e instanceof LivingEntity && !(e instanceof Player) && !e.isDead()) {
						LivingEntity le = (LivingEntity) e;
						double hp = le.getHealth() + 25;
						double max = le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
						if (hp >= max) {
							le.setHealth(max);
						} else {
							le.setHealth(hp);
						}
					}
				}
			}
		);
	}


}

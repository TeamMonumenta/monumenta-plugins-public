package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class SpellRage extends SpellBaseAoE {

	public static boolean isCasting = false;

	public SpellRage(Plugin plugin, LivingEntity launcher, int radius, int time) {
		super(plugin, launcher, radius, time, 160, false, Sound.UI_TOAST_OUT,
			(Location loc) -> {
				World world = loc.getWorld();

				if (!isCasting) {
					world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1f, 0.6f);
					isCasting = true;
				}

				world.spawnParticle(Particle.SPELL_WITCH, loc, 1, ((double) radius) / 2, ((double) radius) / 2, ((double) radius) / 2, 0.05);
			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.spawnParticle(Particle.CRIT_MAGIC, loc, 1, 0.25, 0.25, 0.25, 0.1);
			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.5f);
				world.playSound(loc, Sound.ENTITY_RAVAGER_HURT, 1.5f, 0.5f);
				world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.75f);
			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.spawnParticle(Particle.SPELL_WITCH, loc, 1, 0.1, 0.1, 0.1, 0.3);
				world.spawnParticle(Particle.BUBBLE_POP, loc, 2, 0.25, 0.25, 0.25, 0.1);
			},
			(Location loc) -> {
				for (LivingEntity mob : EntityUtils.getNearbyMobs(launcher.getLocation(), radius)) {
					PotionUtils.applyPotion(mob, mob, new PotionEffect(PotionEffectType.SPEED, 120, 0, true, false));
					PotionUtils.applyPotion(mob, mob, new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 120, 1, true, false));
					mob.getWorld().spawnParticle(Particle.SPELL_WITCH, mob.getLocation().add(0, mob.getHeight() / 2, 0), 15, 0.25, 0.45, 0.25, 1);
					mob.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, mob.getLocation().add(0, mob.getHeight() / 2, 0), 5, 0.35, 0.5, 0.35, 0);
				}

				isCasting = false;
			}
		);
	}
}

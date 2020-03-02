package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellAxtalWitherAoe extends SpellBaseAoE {

	public SpellAxtalWitherAoe(Plugin plugin, LivingEntity launcher, int radius, float maxDamage, float minDamage) {
		super(plugin, launcher, radius, 80, 0, true, Sound.ENTITY_CAT_HISS,
			(Location loc) -> {
				World world = loc.getWorld();
				world.spawnParticle(Particle.SPELL_WITCH, loc, 25, 6, 3, 6);
			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.spawnParticle(Particle.SMOKE_LARGE, loc, 1, 0.25, 0.25, 0.25, 0, null, true);
			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 3, 0.5f);
				world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 3, 1f);
				world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 3, 1.5f);
				world.spawnParticle(Particle.SMOKE_LARGE, loc, 125, 0, 0, 0, 0.5, null, true);
			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.spawnParticle(Particle.SMOKE_NORMAL, loc, 4, 0.25, 0.25, 0.25, 0.35, null, true);
			},
			(Location loc) -> {
				for (Player player : PlayerUtils.playersInRange(launcher.getLocation(), radius)) {
					double distance = player.getLocation().distance(launcher.getLocation());
					BossUtils.bossDamage(launcher, player, ((maxDamage - minDamage) * ((radius - distance) / radius)) + minDamage);
					player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 1));
				}
			}
		);
	}
}

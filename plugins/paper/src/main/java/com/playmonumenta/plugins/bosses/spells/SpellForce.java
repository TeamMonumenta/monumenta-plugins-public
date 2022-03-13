package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpellForce extends SpellBaseAoE {

	public SpellForce(Plugin plugin, LivingEntity launcher, int radius, int time, int cooldown, boolean needNearPlayers, ChargeAuraAction chargeAura, ChargeCircleAction chargeCircle, OutburstAction outburst, CircleOutburstAction circleOutburstAction, DealDamageAction damageAction) {
		super(plugin, launcher, radius, time, cooldown, true, needNearPlayers, Sound.ENTITY_IRON_GOLEM_ATTACK, 1, 1, chargeAura, chargeCircle, outburst, circleOutburstAction, damageAction);
	}

	public SpellForce(Plugin plugin, LivingEntity launcher, int radius, int time, int cooldown, ChargeAuraAction chargeAura, ChargeCircleAction chargeCircle, OutburstAction outburst, CircleOutburstAction circleOutburstAction, DealDamageAction damageAction) {
		this(plugin, launcher, radius, time, cooldown, true, chargeAura, chargeCircle, outburst, circleOutburstAction, damageAction);
	}

	public SpellForce(Plugin plugin, LivingEntity launcher, int radius, int time) {
		this(plugin, launcher, radius, time, 160,
		(Location loc) -> {
			World world = loc.getWorld();
			world.spawnParticle(Particle.SMOKE_LARGE, loc, 1, ((double) radius) / 2, ((double) radius) / 2, ((double) radius) / 2, 0.05);
		},
		(Location loc) -> {
			World world = loc.getWorld();
			world.spawnParticle(Particle.CRIT_MAGIC, loc, 1, 0.25, 0.25, 0.25, 0.1);
		},
		(Location loc) -> {
			World world = loc.getWorld();
			world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.65f);
			world.playSound(loc, Sound.ENTITY_GHAST_SHOOT, 1f, 0.5f);
			world.playSound(loc, Sound.ENTITY_GUARDIAN_HURT, 1f, 0.8f);
			world.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 0.5, 0), 100, 0.5, 0, 0.5, 0.8f);
		},
		(Location loc) -> {
			World world = loc.getWorld();
			world.spawnParticle(Particle.SMOKE_LARGE, loc, 1, 0.1, 0.1, 0.1, 0.3);
			world.spawnParticle(Particle.SMOKE_NORMAL, loc, 2, 0.25, 0.25, 0.25, 0.1);
		},
		(Location loc) -> {
			for (Player player : PlayerUtils.playersInRange(launcher.getLocation(), radius, true)) {

				double distance = player.getLocation().distance(loc);
				if (distance < radius / 3.0) {
					player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 2));
					MovementUtils.knockAway(launcher, player, 3.0f, false);
				} else if (distance < (radius * 2.0) / 3.0) {
					player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 1));
					MovementUtils.knockAway(launcher, player, 2.1f, false);
				} else if (distance < radius) {
					player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 0));
					MovementUtils.knockAway(launcher, player, 1.2f, false);
				}
				player.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, player.getLocation().clone().add(0, 1, 0), 4, 0.25, 0.5, 0.25, 0);
			}
		});
	}
}

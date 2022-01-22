package com.playmonumenta.plugins.depths.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.SpellBaseAoE;
import com.playmonumenta.plugins.effects.AbilitySilence;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SpellVoidBlast extends SpellBaseAoE {

	public static final int CAST_DURATION = 3 * 20;
	public static final int SILENCE_DURATION = 5 * 20;
	public static final int RADIUS = 5;
	public static final int MIN_DAMAGE = 25;
	public static final int MAX_DAMAGE = 40;


	public SpellVoidBlast(Plugin plugin, LivingEntity launcher, int radius, float maxDamage, float minDamage, int duration, int cooldown) {
		super(plugin, launcher, radius, duration, cooldown, false, Sound.ENTITY_VEX_CHARGE,
			(Location loc) -> {
				World world = loc.getWorld();
				world.spawnParticle(Particle.CLOUD, loc, 3, ((double) radius) / 2, ((double) radius) / 2, ((double) radius) / 2, 0.05);
			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.spawnParticle(Particle.SPELL_WITCH, loc, 1, 0.25, 0.25, 0.25, 0);
			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.65F);
			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.spawnParticle(Particle.CLOUD, loc, 1, 0.1, 0.1, 0.1, 0.2);
				world.spawnParticle(Particle.SPELL_WITCH, loc, 1, 0.25, 0.25, 0.25, 0);
			},
			(Location loc) -> {
				for (Player player : PlayerUtils.playersInRange(launcher.getLocation(), radius, true)) {
					double distance = player.getLocation().distance(launcher.getLocation());
					BossUtils.blockableDamage(launcher, player, DamageType.MAGIC, ((maxDamage - minDamage) * ((radius - distance) / radius)) + minDamage, "Void Blast", launcher.getLocation());
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, "Void Blast", new AbilitySilence(SILENCE_DURATION));
				}
			}
		);
	}

	public SpellVoidBlast(Plugin plugin, LivingEntity launcher, int cooldownTicks) {
		this(plugin, launcher, RADIUS, MIN_DAMAGE, MAX_DAMAGE, CAST_DURATION, cooldownTicks);
	}
}

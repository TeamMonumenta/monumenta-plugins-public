package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.utils.AbilityUtils;

public class AbilitySilenceBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_abilitysilence";
	public static final int detectionRange = 32;

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(127, 0, 0), 1.0f);

	private static final int DURATION = 20 * 3;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new AbilitySilenceBoss(plugin, boss);
	}

	public AbilitySilenceBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super.constructBoss(plugin, identityTag, boss, null, null, detectionRange, null);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		// Attack was blocked
		if (event.getFinalDamage() == 0) {
			return;
		}

		LivingEntity target = (LivingEntity) event.getEntity();
		if (target instanceof Player) {
			World world = target.getWorld();
			Location loc = target.getLocation().add(0, 1, 0);
			world.playSound(loc, Sound.BLOCK_PORTAL_TRIGGER, 0.25f, 2f);
			world.spawnParticle(Particle.REDSTONE, loc, 100, 0, 0, 0, 0.5, COLOR);

			AbilityUtils.silencePlayer((Player) target, DURATION);
		}
	}
}

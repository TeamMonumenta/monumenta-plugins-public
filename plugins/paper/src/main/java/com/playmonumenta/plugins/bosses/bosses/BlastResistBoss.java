package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.Plugin;

public class BlastResistBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_blastresist";
	public static final int detectionRange = 100;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new BlastResistBoss(plugin, boss);
	}

	public BlastResistBoss(Plugin plugin, LivingEntity boss) {
		super.constructBoss(plugin, identityTag, boss, null, null, detectionRange, null);
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.BLOCK_EXPLOSION || event.getCause() == DamageCause.ENTITY_EXPLOSION) {
			event.setDamage(event.getDamage() * 0.1);
		}
	}
}

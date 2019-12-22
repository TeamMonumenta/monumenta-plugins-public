package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

public class PlayerDamageOnlyBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_player_dmg_only";
	public static final int detectionRange = 50;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new PlayerDamageOnlyBoss(plugin, boss);
	}

	public PlayerDamageOnlyBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super.constructBoss(plugin, identityTag, boss, null, null, detectionRange, null);
	}

	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			event.setCancelled(true);
		}
	}
}


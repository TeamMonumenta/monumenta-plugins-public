package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.utils.AbilityUtils;

public class SilenceOnHitBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_silence";
	public static final int detectionRange = 30;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) {
		return new SilenceOnHitBoss(plugin, boss);
	}

	public SilenceOnHitBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		super.constructBoss(null, null, detectionRange, null);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		LivingEntity target = (LivingEntity) event.getEntity();

		if (target instanceof Player) {
			AbilityUtils.silencePlayer((Player)target, 5 * 20);
		}
	}
}

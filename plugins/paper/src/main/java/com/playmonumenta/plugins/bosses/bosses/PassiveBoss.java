package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityTargetEvent;

public final class PassiveBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_passive";

	public PassiveBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		event.setCancelled(true);
	}
}


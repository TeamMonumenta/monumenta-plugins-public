package com.playmonumenta.plugins.bosses.bosses;

import java.util.Collections;

import com.playmonumenta.plugins.bosses.SpellManager;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

public class NoExperienceBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_no_exp";
	public static final int detectionRange = 30;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new NoExperienceBoss(plugin, boss);
	}

	public NoExperienceBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		// Boss effectively does nothing
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void death(EntityDeathEvent event) {
		if (event != null) {
			event.setDroppedExp(0);
		}
	}
}

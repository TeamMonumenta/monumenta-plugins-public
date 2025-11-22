package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.Collections;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;

public final class AntiSuffocationBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_antisuffocation";
	public static final int detectionRange = 60;

	public AntiSuffocationBoss(final Plugin plugin, final LivingEntity boss) {
		super(plugin, identityTag, boss);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onHurt(final DamageEvent event) {
		if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
			event.setCancelled(true);
		}
	}
}

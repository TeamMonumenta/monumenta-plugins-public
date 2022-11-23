package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.Collections;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public final class CancelDamageBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_canceldamage";
	public static final int detectionRange = 50;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new CancelDamageBoss(plugin, boss);
	}

	public CancelDamageBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		event.setCancelled(true);
	}
}


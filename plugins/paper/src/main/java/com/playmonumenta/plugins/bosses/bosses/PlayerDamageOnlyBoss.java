package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.Collections;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PlayerDamageOnlyBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_player_dmg_only";
	public static final int detectionRange = 50;

	public PlayerDamageOnlyBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (!(damagee instanceof Player)) {
			event.setCancelled(true);
		}
	}
}


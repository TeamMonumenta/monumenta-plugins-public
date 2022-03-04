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

	//This tag is on mobs from summon abilities (Hunting Companion, Metalmancy, etc.)
	public static final String bypassTag = "bypass_player_damage_only";

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new PlayerDamageOnlyBoss(plugin, boss);
	}

	public PlayerDamageOnlyBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (event.getSource() != null && !(event.getSource() instanceof Player) && !event.getSource().getScoreboardTags().contains(bypassTag)) {
			event.setCancelled(true);
		}
	}
}


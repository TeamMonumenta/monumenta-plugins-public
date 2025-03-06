package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.SpellHandSwap;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;

public class HandSwapBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_handswap";
	public static final int detectionRange = 35;

	public HandSwapBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		if (!(boss instanceof Mob mob)) {
			throw new Exception(identityTag + " only works on mobs! Entity name='" + boss.getName() + "', tags=[" + String.join(",", boss.getScoreboardTags()) + "]");
		}

		super.constructBoss(new SpellHandSwap(mob), detectionRange);
	}
}

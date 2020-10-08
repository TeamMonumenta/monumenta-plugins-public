package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellCrowdControlClear;

public class CrowdControlResistanceBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_cleanse";
	public static final int detectionRange = 100;

	private static final int CLEAR_TIME = 20 * 4;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new CrowdControlResistanceBoss(plugin, boss);
	}


	public CrowdControlResistanceBoss(Plugin plugin, LivingEntity boss) {
		List<Spell> passive = Arrays.asList(new SpellCrowdControlClear(boss, CLEAR_TIME));

		super.constructBoss(plugin, identityTag, boss, null, passive, detectionRange, null);
	}
}

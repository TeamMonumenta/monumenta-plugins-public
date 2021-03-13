package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellTffBookSummon;

public class TffBookSummonBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_tffbooksummon";
	public static final int detectionRange = 30;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new TffBookSummonBoss(plugin, boss);
	}

	public TffBookSummonBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
		                                                 new SpellTffBookSummon(plugin, boss)
		                                             ));

		super.constructBoss(activeSpells, null, detectionRange, null);
	}
}

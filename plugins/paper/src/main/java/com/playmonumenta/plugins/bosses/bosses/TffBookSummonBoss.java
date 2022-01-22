package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellTffBookSummon;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public class TffBookSummonBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_tffbooksummon";
	public static final int detectionRange = 30;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new TffBookSummonBoss(plugin, boss);
	}

	public TffBookSummonBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		List<Spell> passiveSpells = Arrays.asList(
										new SpellTffBookSummon(plugin, boss)
										);

		super.constructBoss(null, passiveSpells, detectionRange, null);
	}
}

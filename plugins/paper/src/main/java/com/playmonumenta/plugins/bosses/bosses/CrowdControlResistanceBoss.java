package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellCrowdControlClear;
import com.playmonumenta.plugins.utils.BossUtils;

public class CrowdControlResistanceBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_cleanse";

	public static class Parameters extends BossParameters {
		public int CLEAR_TIME = 20 * 4;
		public int DETECTION = 100;
	}


	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new CrowdControlResistanceBoss(plugin, boss);
	}


	public CrowdControlResistanceBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		List<Spell> passive = Arrays.asList(new SpellCrowdControlClear(boss, p.CLEAR_TIME));

		super.constructBoss(null, passive, p.DETECTION, null);
	}
}

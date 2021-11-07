package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.LivingEntity;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellLandSlow;
import com.playmonumenta.plugins.utils.BossUtils;

public class LandSlowBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_land_slow";
	public static final int detectionRange = 40;

	public static class Parameters extends BossParameters {
		public double SLOWNESSPERCENT = 1;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new LandSlowBoss(plugin, boss);
	}

	public LandSlowBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		List<Spell> passiveSpells = Arrays.asList(new SpellLandSlow(plugin, boss, p.SLOWNESSPERCENT));

		super.constructBoss(null, passiveSpells, detectionRange, null);
	}
}

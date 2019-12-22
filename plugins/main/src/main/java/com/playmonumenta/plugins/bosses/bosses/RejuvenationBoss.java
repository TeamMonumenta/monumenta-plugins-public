package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellMobHealAoE;

public class RejuvenationBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_rejuvenation";
	public static final int detectionRange = 20;

	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new RejuvenationBoss(plugin, boss);
	}

	public RejuvenationBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		SpellManager activeSpells = new SpellManager(Arrays.asList(
		                                                 new SpellMobHealAoE(plugin, boss)
		                                             ));

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, null, detectionRange, null);
	}
}

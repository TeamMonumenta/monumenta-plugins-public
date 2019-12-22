package com.playmonumenta.bossfights.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.bossfights.SpellManager;
import com.playmonumenta.bossfights.spells.SpellCyanSummon;

public class CyanSummonBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_cyansummon";
	public static final int detectionRange = 30;

	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new CyanSummonBoss(plugin, boss);
	}

	public CyanSummonBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		SpellManager activeSpells = new SpellManager(Arrays.asList(
		                                                 new SpellCyanSummon(plugin, mBoss)
		                                             ));

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, null, detectionRange, null);
	}
}

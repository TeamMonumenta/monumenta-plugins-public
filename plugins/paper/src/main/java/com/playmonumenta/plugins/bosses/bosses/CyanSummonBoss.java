package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.Collections;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellCyanSummon;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class CyanSummonBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_cyansummon";
	public static final int detectionRange = 30;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new CyanSummonBoss(plugin, boss);
	}

	public CyanSummonBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
		                                                 new SpellCyanSummon(boss)
		                                             ));

		super.constructBoss(activeSpells, Collections.emptyList(), detectionRange, null);
	}
}

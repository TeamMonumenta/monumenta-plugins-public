package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.SpellCyanSummon;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class CyanSummonBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_cyansummon";
	public static final int detectionRange = 30;

	public CyanSummonBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		super.constructBoss(new SpellCyanSummon(boss), detectionRange);
	}
}

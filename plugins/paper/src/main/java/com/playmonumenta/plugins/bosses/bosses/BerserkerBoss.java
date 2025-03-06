package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.SpellBerserk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public final class BerserkerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_berserker";
	public static final int detectionRange = 35;

	public BerserkerBoss(final Plugin plugin, final LivingEntity boss) {
		super(plugin, identityTag, boss);

		super.constructBoss(new SpellBerserk(mBoss), detectionRange);
	}
}

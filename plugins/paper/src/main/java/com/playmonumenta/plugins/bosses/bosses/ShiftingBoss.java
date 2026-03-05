package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.SpellShiftingSpeed;
import org.bukkit.entity.LivingEntity;

public class ShiftingBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_shifting";
	public static final int detectionRange = 30;

	public ShiftingBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		super.constructBoss(new SpellShiftingSpeed(boss), detectionRange);
	}
}

package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellShiftingSpeed;

public class ShiftingBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_shifting";
	public static final int detectionRange = 30;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ShiftingBoss(plugin, boss);
	}

	public ShiftingBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellShiftingSpeed(boss)
		));

		super.constructBoss(activeSpells, null, detectionRange, null);
	}
}

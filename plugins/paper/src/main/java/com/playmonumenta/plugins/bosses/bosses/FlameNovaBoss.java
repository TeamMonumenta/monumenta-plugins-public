package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellFlameNova;

public class FlameNovaBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_flamenova";
	public static final int detectionRange = 20;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FlameNovaBoss(plugin, boss);
	}

	public FlameNovaBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellFlameNova(plugin, boss, 9, 70)
		));

		super.constructBoss(activeSpells, null, detectionRange, null);
	}
}

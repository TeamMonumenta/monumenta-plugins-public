package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellFrostNova;

public class FrostNovaBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_frostnova";
	public static final int detectionRange = 20;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FrostNovaBoss(plugin, boss);
	}

	public FrostNovaBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellFrostNova(plugin, boss, 8, 18, 18)
		));

		super.constructBoss(activeSpells, null, detectionRange, null);
	}
}

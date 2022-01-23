package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellFloat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class FloatBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_float";
	private static final int detectionRange = 16;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FloatBoss(plugin, boss);
	}

	public FloatBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		List<Spell> passiveSpells = Arrays.asList(
		                                new SpellFloat(boss)
		                            );

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}
}

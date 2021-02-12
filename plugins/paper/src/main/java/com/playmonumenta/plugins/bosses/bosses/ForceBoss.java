package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellForce;

public class ForceBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_force";
	public static final int detectionRange = 20;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ForceBoss(plugin, boss);
	}

	public ForceBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellForce(plugin, boss, 5, 70)
		));

		super.constructBoss(activeSpells, null, detectionRange, null);
	}
}

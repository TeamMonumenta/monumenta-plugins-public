package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.Collections;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBombToss;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class BombTossBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_bombtoss";
	public static final int detectionRange = 20;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new BombTossBoss(plugin, boss);
	}

	public BombTossBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBombToss(plugin, boss, detectionRange)
		));

		super.constructBoss(activeSpells, Collections.emptyList(), detectionRange, null);
	}
}

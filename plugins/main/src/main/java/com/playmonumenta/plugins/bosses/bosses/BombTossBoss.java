package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBombToss;

public class BombTossBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_bombtoss";
	public static final int detectionRange = 20;

	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new BombTossBoss(plugin, boss);
	}

	public BombTossBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBombToss(plugin, mBoss, detectionRange)
		));

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, null, detectionRange, null);
	}
}

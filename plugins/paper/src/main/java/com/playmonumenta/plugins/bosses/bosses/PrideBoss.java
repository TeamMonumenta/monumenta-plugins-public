package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellShadowSpike;
import com.playmonumenta.plugins.bosses.spells.SpellShadowThorns;

public class PrideBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_pride";
	public static final int detectionRange = 20;

	private static final int COOLDOWN_SPIKE = 20 * 5;
	private static final int COOLDOWN_THORNS = 20 * 10;
	private static final int DAMAGE = 24;


	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new PrideBoss(plugin, boss);
	}

	public PrideBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		SpellManager activeSpells = new SpellManager(Arrays.asList(
				new SpellShadowSpike(plugin, boss, COOLDOWN_SPIKE, DAMAGE),
				new SpellShadowThorns(plugin, boss, COOLDOWN_THORNS, DAMAGE)
		));

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, null, detectionRange, null);
	}
}

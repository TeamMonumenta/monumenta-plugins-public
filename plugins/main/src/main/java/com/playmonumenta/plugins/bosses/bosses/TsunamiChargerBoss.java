package com.playmonumenta.bossfights.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.bossfights.SpellManager;
import com.playmonumenta.bossfights.spells.SpellTsunamiCharge;

public class TsunamiChargerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_tsunamicharger";
	public static final int detectionRange = 20;

	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new TsunamiChargerBoss(plugin, boss);
	}

	public TsunamiChargerBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellTsunamiCharge(plugin, mBoss, detectionRange, 15.0F)
		));

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, null, detectionRange, null);
	}
}

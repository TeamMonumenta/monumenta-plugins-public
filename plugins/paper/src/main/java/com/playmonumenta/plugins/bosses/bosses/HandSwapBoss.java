package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.Collections;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellHandSwap;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;

public class HandSwapBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_handswap";
	public static final int detectionRange = 35;

	Mob mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new HandSwapBoss(plugin, boss);
	}

	public HandSwapBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		if (!(boss instanceof Mob)) {
			throw new Exception("boss_handswap only works on mobs!");
		}

		mBoss = (Mob)boss;

		SpellManager activeSpells = new SpellManager(Arrays.asList(
		    new SpellHandSwap(mBoss)
		));

		super.constructBoss(activeSpells, Collections.emptyList(), detectionRange, null);
	}
}

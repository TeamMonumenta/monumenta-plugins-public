package com.playmonumenta.bossfights.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import com.playmonumenta.bossfights.Plugin;
import com.playmonumenta.bossfights.SpellManager;
import com.playmonumenta.bossfights.spells.SpellHandSwap;

public class HandSwapBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_handswap";
	public static final int detectionRange = 35;

	Mob mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new HandSwapBoss(plugin, boss);
	}

	public HandSwapBoss(Plugin plugin, LivingEntity boss) throws Exception {
		if (!(boss instanceof Mob)) {
			throw new Exception("boss_handswap only works on mobs!");
		}

		mBoss = (Mob)boss;

		SpellManager activeSpells = new SpellManager(Arrays.asList(
		    new SpellHandSwap(mBoss)
		));

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, null, detectionRange, null);
	}
}

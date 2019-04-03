package com.playmonumenta.bossfights.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import com.playmonumenta.bossfights.Plugin;
import com.playmonumenta.bossfights.SpellManager;
import com.playmonumenta.bossfights.spells.SpellTargetVisiblePlayer;

public class PlayerTargetBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_targetplayer";
	public static final int detectionRange = 30;

	Mob mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new PlayerTargetBoss(plugin, boss);
	}

	public PlayerTargetBoss(Plugin plugin, LivingEntity boss) throws Exception {
		if (!(boss instanceof Mob)) {
			throw new Exception("boss_targetplayer only works on mobs!");
		}

		mBoss = (Mob)boss;

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellTargetVisiblePlayer(mBoss, detectionRange, 60, 160)
		));

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, null, detectionRange, null);
	}
}


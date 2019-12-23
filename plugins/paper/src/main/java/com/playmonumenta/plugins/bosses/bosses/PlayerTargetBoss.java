package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.Dolphin;
import org.bukkit.entity.Golem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Wolf;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.bosses.spells.SpellTargetVisiblePlayer;

public class PlayerTargetBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_targetplayer";
	public static final int detectionRange = 30;

	Mob mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new PlayerTargetBoss(plugin, boss);
	}

	public PlayerTargetBoss(Plugin plugin, LivingEntity boss) throws Exception {
		if (!(boss instanceof Mob)) {
			throw new Exception(identityTag + " only works on mobs!");
		}

		if (boss instanceof Wolf || boss instanceof Golem || boss instanceof Dolphin || boss instanceof Ocelot) {
			boss.setRemoveWhenFarAway(true);
		}

		mBoss = (Mob)boss;

		Spell tgt = new SpellTargetVisiblePlayer(mBoss, detectionRange, 60, 160);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellRunAction(() -> {
				if (boss instanceof Wolf && ((Wolf)boss).isTamed()) {
					((Wolf)boss).setAngry(false);
				} else {
					tgt.run();
				}
			})
		));

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, null, detectionRange, null);
	}
}


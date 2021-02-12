package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellTpBehindTargetedPlayer;

public class TpBehindTargetedBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_tpbehindtargeted";
	public static final int detectionRange = 20;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new TpBehindTargetedBoss(plugin, boss);
	}

	public TpBehindTargetedBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellTpBehindTargetedPlayer(plugin, boss, 240)));


		super.constructBoss(activeSpells, null, detectionRange, null);
	}
}

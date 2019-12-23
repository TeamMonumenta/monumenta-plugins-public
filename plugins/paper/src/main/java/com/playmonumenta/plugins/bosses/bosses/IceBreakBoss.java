package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellIceBreak;

public class IceBreakBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_icebreak";
	public static final int detectionRange = 40;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new IceBreakBoss(plugin, boss);
	}

	public IceBreakBoss(Plugin plugin, LivingEntity boss) {
		List<Spell> passiveSpells = Arrays.asList(new SpellIceBreak(boss));

		boss.setRemoveWhenFarAway(false);
		super.constructBoss(plugin, identityTag, boss, null, passiveSpells, detectionRange, null);
	}
}

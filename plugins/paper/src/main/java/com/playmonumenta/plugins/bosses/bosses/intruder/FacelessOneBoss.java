package com.playmonumenta.plugins.bosses.bosses.intruder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellFacelessOne;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

public class FacelessOneBoss extends BossAbilityGroup {
	public static String identityTag = "boss_facelessone";

	public FacelessOneBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		constructBoss(new SpellFacelessOne(plugin, (Mob) boss), 100);
	}
}

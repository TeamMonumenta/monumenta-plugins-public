package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellTsunamiCharge;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class TsunamiChargerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_tsunamicharger";
	public static final int detectionRange = 20;

	public TsunamiChargerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Spell spell = new SpellTsunamiCharge(plugin, boss, detectionRange, 15.0F);

		super.constructBoss(spell, detectionRange);
	}
}

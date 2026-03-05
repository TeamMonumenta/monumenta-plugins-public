package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellTsunamiCharge;
import org.bukkit.entity.LivingEntity;

/* TODO: Merge this into ChargerBoss */
public class TsunamiChargerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_tsunamicharger";
	public static final int detectionRange = 20;

	public TsunamiChargerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Spell spell = new SpellTsunamiCharge(plugin, boss, detectionRange, 15.0F);

		super.constructBoss(spell, detectionRange);
	}
}

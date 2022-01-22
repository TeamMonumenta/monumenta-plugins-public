package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellTsunamiCharge;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;

public class TsunamiChargerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_tsunamicharger";
	public static final int detectionRange = 20;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new TsunamiChargerBoss(plugin, boss);
	}

	public TsunamiChargerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellTsunamiCharge(plugin, boss, detectionRange, 15.0F)
		));

		super.constructBoss(activeSpells, null, detectionRange, null);
	}
}

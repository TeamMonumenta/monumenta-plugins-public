package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellGenericCharge;
import com.playmonumenta.plugins.utils.BossUtils;

public class ChargerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_charger";

	public static class Parameters {
		public int COOLDOWN = 8 * 20;
		public int DETECTION = 20;
		public int DELAY = 5 * 20;
		public float DAMAGE = 15;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ChargerBoss(plugin, boss);
	}

	public ChargerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellGenericCharge(plugin, boss, p.DETECTION, p.DAMAGE, p.COOLDOWN)
		));

		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);
	}
}

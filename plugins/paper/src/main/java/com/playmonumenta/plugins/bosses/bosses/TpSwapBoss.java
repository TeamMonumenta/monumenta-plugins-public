package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellTpSwapPlaces;
import com.playmonumenta.plugins.utils.BossUtils;

public class TpSwapBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_tpswap";

	public static class Parameters {
		public int RANGE = 6;
		public int DELAY = 100;
		public int DURATION = 50;
		public int DETECTION = 20;
		public int COOLDOWN = 12 * 20;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new TpSwapBoss(plugin, boss);
	}

	public TpSwapBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellTpSwapPlaces(plugin, boss, p.COOLDOWN, p.RANGE, p.DURATION)));


		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);
	}
}

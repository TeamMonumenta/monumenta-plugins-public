package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellForce;
import com.playmonumenta.plugins.utils.BossUtils;

public class ForceBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_force";

	public static class Parameters {
		public int DETECTION = 20;
		public int COOLDOWN = 20;
		public int DURATION = 70;
		public int RADIUS = 5;
		public int DELAY = 100;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ForceBoss(plugin, boss);
	}

	public ForceBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellForce(plugin, boss, p.RADIUS, p.DURATION, p.COOLDOWN)
		));

		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);
	}
}

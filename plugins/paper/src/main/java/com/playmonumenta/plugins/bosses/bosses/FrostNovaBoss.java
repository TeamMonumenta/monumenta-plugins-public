package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellFrostNova;
import com.playmonumenta.plugins.utils.BossUtils;

public class FrostNovaBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_frostnova";

	public static class Parameters {
		public int RADIUS = 8;
		public int DELAY = 100;
		public int DAMAGE = 18;
		public int DURATION = 80;
		public int COOLDOWN = 160;
		public int DETECTION = 20;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FrostNovaBoss(plugin, boss);
	}

	public FrostNovaBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellFrostNova(plugin, boss, p.RADIUS, p.DAMAGE, p.DAMAGE, p.DURATION, p.COOLDOWN)
		));

		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);
	}
}

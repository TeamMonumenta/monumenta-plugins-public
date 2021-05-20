package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellFlameNova;
import com.playmonumenta.plugins.utils.BossUtils;

public class FlameNovaBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_flamenova";

	public static class Parameters {
		public int RANGE = 9;
		public int DELAY = 100;
		public int DAMAGE = 17;
		public int DETECTION = 20;
		public int COOLDOWN = 160;
		public int FUSE_TIME = 70;
		public int FIRE_DURATION = 80;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FlameNovaBoss(plugin, boss);
	}

	public FlameNovaBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellFlameNova(plugin, boss, p.RANGE, p.FUSE_TIME, p.COOLDOWN, p.DAMAGE, p.FIRE_DURATION)
		));

		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);
	}
}

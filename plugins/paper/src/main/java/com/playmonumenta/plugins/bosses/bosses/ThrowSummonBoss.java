package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.SpellThrowSummon;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.Collections;

public class ThrowSummonBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_throwsummon";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int LOBS = 1;

		@BossParam(help = "not written")
		public double RADIUS = 8;

		@BossParam(help = "not written")
		public int DETECTION = 20;

		@BossParam(help = "not written")
		public int COOLDOWN = 160;

		@BossParam(help = "not written")
		public String SPAWNEDMOB = "LostSoul";

		@BossParam(help = "Delay of the spell")
		public int DELAY = 100;

	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ThrowSummonBoss(plugin, boss);
	}

	public ThrowSummonBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellThrowSummon(plugin, boss, p.DETECTION, p.LOBS, p.COOLDOWN, p.SPAWNEDMOB)));


		super.constructBoss(activeSpells, Collections.emptyList(), p.DETECTION, null, p.DELAY);
	}
}

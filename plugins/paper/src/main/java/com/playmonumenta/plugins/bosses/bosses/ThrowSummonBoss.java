package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellThrowSummon;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

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

		@BossParam(help = "Is the spawnedmob from a pool?")
		public boolean POOL = false;

		@BossParam(help = "Delay of the spell")
		public int DELAY = 100;

		@BossParam(help = "Delay between each mob throw, in ticks")
		public int LOB_DELAY = 15;

	}

	public ThrowSummonBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		Spell spell = new SpellThrowSummon(plugin, boss, p.DETECTION, p.LOBS, p.COOLDOWN, p.SPAWNEDMOB, p.POOL, p.LOB_DELAY);


		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}

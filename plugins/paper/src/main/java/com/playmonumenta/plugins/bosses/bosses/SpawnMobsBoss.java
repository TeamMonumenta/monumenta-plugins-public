package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellSpawnMobs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class SpawnMobsBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_spawnmobs";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public double RANGE = 9;
		@BossParam(help = "Minimum Distance from Boss to spawn each mob.")
		public double MIN_RANGE = 0;
		@BossParam(help = "not written")
		public int DELAY = 100;
		@BossParam(help = "not written")
		public int DETECTION = 20;
		@BossParam(help = "not written")
		public int COOLDOWN = 160;
		@BossParam(help = "Whether it needs to have line of sight or not")
		public boolean LINEOFSIGHT = false;
		@BossParam(help = "not written")
		public String SPAWNEDMOB = "";
		@BossParam(help = "not written")
		public int SPAWNCOUNT = 0;
		@BossParam(help = "Maximum Mobs within range (param MOB_CAP_RANGE) where the ability fails to spawn mobs (Default = 15)")
		public int MOB_CAP = 15;
		@BossParam(help = "Radius of mobs counted for the mob cap check where the ability fails to spawn mobs (Default = 10)")
		public double MOB_CAP_RANGE = 10;
		@BossParam(help = "whether or not to cap mobs by name")
		public boolean CAP_MOBS_BY_NAME = false;
		@BossParam(help = "name of the mob checked when checking for mob cap. include spaces if needed!")
		public String MOB_CAP_NAME = "";
	}

	public SpawnMobsBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		Spell spell = new SpellSpawnMobs(boss, p.SPAWNCOUNT, p.SPAWNEDMOB, p.COOLDOWN, p.RANGE, p.MIN_RANGE, p.LINEOFSIGHT, p.DETECTION, p.MOB_CAP, p.MOB_CAP_RANGE, p.CAP_MOBS_BY_NAME, p.MOB_CAP_NAME);

		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}

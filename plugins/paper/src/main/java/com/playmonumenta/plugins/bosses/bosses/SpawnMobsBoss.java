package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.SpellSpawnMobs;
import com.playmonumenta.plugins.utils.BossUtils;

public class SpawnMobsBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_spawnmobs";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int RANGE = 9;
		@BossParam(help = "not written")
		public int DELAY = 100;
		@BossParam(help = "not written")
		public int DETECTION = 20;
		@BossParam(help = "not written")
		public int COOLDOWN = 160;
		@BossParam(help = "not written")
		public String SPAWNEDMOB = "";
		@BossParam(help = "not written")
		public int SPAWNCOUNT = 0;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new SpawnMobsBoss(plugin, boss);
	}

	public SpawnMobsBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellSpawnMobs(boss, p.SPAWNCOUNT, p.SPAWNEDMOB, p.COOLDOWN, p.RANGE)
		));

		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);
	}
}

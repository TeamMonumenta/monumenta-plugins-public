package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellSpawnMobs;
import com.playmonumenta.plugins.utils.BossUtils;

public class SpawnMobsBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_spawnmobs";

	public static class Parameters {
		public int RANGE = 9;
		public int DELAY = 100;
		public int DETECTION = 20;
		public int COOLDOWN = 160;
		public String SPAWNEDMOB = "";
		public int SPAWNCOUNT = 0;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new SpawnMobsBoss(plugin, boss);
	}

	public SpawnMobsBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellSpawnMobs(boss, p.SPAWNCOUNT, p.SPAWNEDMOB, p.COOLDOWN, p.RANGE)
		));

		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);
	}
}

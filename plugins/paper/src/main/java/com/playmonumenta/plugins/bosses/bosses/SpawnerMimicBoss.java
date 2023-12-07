
package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.SpellSpawnerMimic;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class SpawnerMimicBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_spawnermimic";
	public static final int detectionRange = 40;

	public final SpellSpawnerMimic mSpell;

	public static class Parameters extends BossParameters {
		public int DELAY = 40;
	}

	public SpawnerMimicBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		mSpell = new SpellSpawnerMimic(plugin, boss);
		super.constructBoss(mSpell, detectionRange, null, p.DELAY);
	}
}

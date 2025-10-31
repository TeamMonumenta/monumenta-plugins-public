package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import java.util.Collections;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Vex;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;

public class ReplaceVexBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_replace_vex";
	public ReplaceVexBoss.Parameters mParams;

	public static class Parameters extends BossParameters {
		@BossParam(help = "Pool of mobs to summon")
		public LoSPool POOL = LoSPool.LibraryPool.EMPTY;
	}

	public ReplaceVexBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParams = BossParameters.getParameters(boss, identityTag, new Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), -1, null);
	}

	@Override
	public void bossSummonedVex(CreatureSpawnEvent event, Vex vex) {
		event.setCancelled(true);
		Entity entity = mParams.POOL.spawn(vex.getLocation());
		if (entity instanceof Vex newVex && mBoss instanceof Mob summoner) {
			newVex.setSummoner(summoner);
		}
	}
}

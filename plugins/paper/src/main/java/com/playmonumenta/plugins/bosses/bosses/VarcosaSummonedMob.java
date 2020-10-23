package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

/*
 * This is a "fake" boss ability. It doesn't persist across restarts or chunk loading.
 * It's given to a mob summoned by Varcosa phase 1 to track their deaths later
 *
 * TODO: If we wanted to do this "right", it'd be a stateful boss... which serializes the summoner's
 * UUID so that if chunks do load/unload, this mob would still damage the boss when it dies
 *
 * Since the mist should be loaded constantly while active, this shouldn't be a problem except
 * during daily restarts.
 */
public class VarcosaSummonedMob extends BossAbilityGroup {
	private final VarcosaSummonerBoss mSummoner;

	public VarcosaSummonedMob(Plugin plugin, LivingEntity boss, VarcosaSummonerBoss summoner) {
		mPlugin = plugin;
		mSummoner = summoner;

		super.constructBoss(plugin, "boss_varcosa_summoned", boss, null, null, 100, null);
	}

	@Override
	public void death(EntityDeathEvent event) {
		mSummoner.onSummonKilled();
	}
}

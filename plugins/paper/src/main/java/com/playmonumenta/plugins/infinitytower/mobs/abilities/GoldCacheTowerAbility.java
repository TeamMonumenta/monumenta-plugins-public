package com.playmonumenta.plugins.infinitytower.mobs.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerGameUtils;
import com.playmonumenta.plugins.infinitytower.TowerManager;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;


public class GoldCacheTowerAbility extends TowerAbility {

	public GoldCacheTowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, TowerMob mob, boolean isPlayerMob) {
		super(plugin, identityTag, boss, game, mob, isPlayerMob);

		if (isPlayerMob) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(TowerManager.mPlugin, () -> {
				if (game.mPlayer.mTeam.remove(mob)) {
					TowerGameUtils.addGold(game.mPlayer.mPlayer, 2);
				}
			}, 5);

		}
	}
}

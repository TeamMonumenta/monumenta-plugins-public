package com.playmonumenta.plugins.infinitytower.mobs.abilities;


import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerGameUtils;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class FoolsGoldTowerAbility extends TowerAbility {

	public FoolsGoldTowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, TowerMob mob, boolean isPlayerMob, int gold) {
		super(plugin, identityTag, boss, game, mob, isPlayerMob);
		if (isPlayerMob) {
			Player player = mGame.mPlayer.mPlayer;
			TowerGameUtils.addGold(player, gold);
		}

	}
}

package com.playmonumenta.plugins.infinitytower.mobs.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import org.bukkit.entity.LivingEntity;

public abstract class TowerAbility extends BossAbilityGroup {

	protected final TowerMob mMob;
	protected final TowerGame mGame;
	protected final boolean mIsPlayerMob;

	protected TowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, TowerMob mob, boolean isPlayerMob) {
		super(plugin, identityTag, boss);

		mMob = mob;
		mGame = game;
		mIsPlayerMob = isPlayerMob;

	}

}

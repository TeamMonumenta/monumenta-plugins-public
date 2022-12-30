package com.playmonumenta.plugins.infinitytower.mobs.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public abstract class TowerAbility extends BossAbilityGroup {

	protected final @Nullable TowerMob mMob;
	protected final TowerGame mGame;
	protected final boolean mIsPlayerMob;

	protected TowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, @Nullable TowerMob mob, boolean isPlayerMob) {
		super(plugin, identityTag, boss);

		mMob = mob;
		mGame = game;
		mIsPlayerMob = isPlayerMob;

	}

}

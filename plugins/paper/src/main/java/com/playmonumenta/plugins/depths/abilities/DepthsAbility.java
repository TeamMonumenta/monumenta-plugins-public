package com.playmonumenta.plugins.depths.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.depths.DepthsManager;
import org.bukkit.entity.Player;

public abstract class DepthsAbility extends Ability {

	public static int MAX_RARITY = 6;

	public int mRarity;

	public DepthsAbility(Plugin plugin, Player player, DepthsAbilityInfo<?> info) {
		super(plugin, player, info);
		mRarity = DepthsManager.getInstance().getPlayerLevelInAbility(info.getDisplayName(), player);
	}

	@Override
	public DepthsAbilityInfo<?> getInfo() {
		return (DepthsAbilityInfo<?>) mInfo;
	}

	@Override
	public int getAbilityScore() {
		return mRarity;
	}

	@Override
	public boolean isLevelOne() {
		return false;
	}

	@Override
	public boolean isLevelTwo() {
		return false;
	}

	@Override
	public boolean isEnhanced() {
		return false;
	}

}

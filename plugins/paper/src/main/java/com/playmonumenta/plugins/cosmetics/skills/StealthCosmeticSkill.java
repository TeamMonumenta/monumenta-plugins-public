package com.playmonumenta.plugins.cosmetics.skills;

import com.playmonumenta.plugins.utils.AbilityUtils;
import org.bukkit.entity.Player;

public interface StealthCosmeticSkill extends CosmeticSkill {
	default void applyStealthCosmetic(Player player) {
		AbilityUtils.defaultStealthApply(player);
	}

	default void removeStealthCosmetic(Player player) {
		AbilityUtils.defaultStealthRemove(player);
	}
}

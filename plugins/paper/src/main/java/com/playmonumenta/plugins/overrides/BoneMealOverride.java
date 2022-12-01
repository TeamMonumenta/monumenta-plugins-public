package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ZoneUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

public class BoneMealOverride extends BaseOverride {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		return block == null || canBoneMeal(block.getLocation());
	}

	@Override
	public boolean blockDispenseInteraction(Plugin plugin, Block block, ItemStack dispensed) {
		return canBoneMeal(block.getLocation());
	}

	private boolean canBoneMeal(Location loc) {
		return !ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.BONE_MEAL_DISABLED) && !ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.NO_QUICK_BUILDING);
	}
}

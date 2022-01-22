package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.abilities.steelsage.SteelStallion;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class HorseFoodOverride extends BaseOverride {
	@Override
	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity, ItemStack itemInHand) {
		// Prevent the feeding if the horse is a Steel Stallion or if the item has lore
		return clickedEntity == null || !(clickedEntity instanceof Horse) || (!ItemUtils.hasLore(itemInHand) && !SteelStallion.isSteelStallion(clickedEntity));
	}
}

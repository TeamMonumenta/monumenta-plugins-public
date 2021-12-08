package com.playmonumenta.plugins.overrides;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;

public class GoldenAppleOverride extends BaseOverride {
	@Override
	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity, ItemStack itemInHand) {
		// Need an extra check for golden apples, and can't put them into two overrides
		HorseFoodOverride horseFoodOverride = new HorseFoodOverride();
		if (!horseFoodOverride.rightClickEntityInteraction(plugin, player, clickedEntity, itemInHand)) {
			return false;
		}

		return (clickedEntity == null || !(clickedEntity instanceof ZombieVillager));
	}
}

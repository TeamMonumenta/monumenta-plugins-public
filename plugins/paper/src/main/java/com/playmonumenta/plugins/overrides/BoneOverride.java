package com.playmonumenta.plugins.overrides;

import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;

public class BoneOverride extends BaseOverride {
	@Override
	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity, ItemStack item) {
		if (player == null) {
			return true;
		}

		if (player.getGameMode() == GameMode.ADVENTURE) {
			return false;
		}

		// Don't allow non-creative players to feed bones with lore text to wolves
		return (player.getGameMode() == GameMode.CREATIVE || clickedEntity == null || !(clickedEntity instanceof Wolf && item != null && (item.hasItemMeta() || item.getItemMeta().hasLore())));
	}
}

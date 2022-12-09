package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.GameMode;
import org.bukkit.entity.Dolphin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class FishOverride extends BaseOverride {
	@Override
	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity, ItemStack item) {
		if (player == null) {
			return true;
		}

		if (player.getGameMode() == GameMode.ADVENTURE) {
			return false;
		}

		// Don't allow non-creative players to feed fish with lore text to dolphins (Specifically Fruits of the Catch!)
		return player.getGameMode() == GameMode.CREATIVE || clickedEntity == null || !(clickedEntity instanceof Dolphin) || item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore();
	}
}

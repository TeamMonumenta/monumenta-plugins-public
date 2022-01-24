package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import javax.annotation.Nullable;

public class FlowerPotOverride extends BaseOverride {
	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item, Block block, PlayerInteractEvent event) {
		if (player == null) {
			return true;
		}

		if (player.getGameMode() == GameMode.ADVENTURE) {
			return false;
		}

		// Don't allow non-creative players to put saplings with lore text in flower pots
		return (player.getGameMode() == GameMode.CREATIVE || item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore());
	}
}

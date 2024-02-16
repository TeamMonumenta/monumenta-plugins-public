package com.playmonumenta.plugins.overrides;

import com.destroystokyo.paper.MaterialSetTag;
import com.playmonumenta.plugins.Plugin;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CandleCakeOverride extends BaseOverride {
	@Override
	//Prevent candles with lore from being put on cakes
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item, Block block, PlayerInteractEvent event) {
		if (player.getGameMode() == GameMode.ADVENTURE) {
			return false;
		}
		return (player.getGameMode() == GameMode.CREATIVE || item == null || !MaterialSetTag.CANDLES.getValues().contains(item.getType()) || !item.hasItemMeta() || !item.getItemMeta().hasLore());
	}
}

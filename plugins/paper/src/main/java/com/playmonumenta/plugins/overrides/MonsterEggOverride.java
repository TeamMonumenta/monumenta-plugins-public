package com.playmonumenta.plugins.overrides;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ZoneUtils;

public class MonsterEggOverride extends BaseOverride {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		// Only allow creative players or players in their plots (in capital and survival) to use spawn eggs
		if ((player.getGameMode() == GameMode.CREATIVE) || ZoneUtils.isInPlot(player)) {
			return true;
		}
		return false;
	}
}

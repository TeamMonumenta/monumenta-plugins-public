package com.playmonumenta.plugins.overrides;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.safezone.SafeZoneManager.LocationType;

public class MonsterEggOverride extends BaseOverride {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		// Only allow creative players or players in their plots (in capital and survival) to use spawn eggs
		if ((player.getGameMode() == GameMode.CREATIVE) ||
		    ((player.getGameMode() == GameMode.SURVIVAL) && (plugin.mSafeZoneManager.getLocationType(player) == LocationType.Capital))) {
			return true;
		}
		return false;
	}
}

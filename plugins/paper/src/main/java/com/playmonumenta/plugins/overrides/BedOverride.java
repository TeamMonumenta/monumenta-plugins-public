package com.playmonumenta.plugins.overrides;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;

public class BedOverride extends BaseOverride {
	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		return ServerProperties.getIsSleepingEnabled();
	}
}

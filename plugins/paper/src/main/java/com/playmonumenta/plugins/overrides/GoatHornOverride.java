package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class GoatHornOverride extends BaseOverride {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		return false;
	}
}

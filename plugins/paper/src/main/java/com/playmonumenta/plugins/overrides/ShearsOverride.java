package com.playmonumenta.plugins.overrides;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;

public class ShearsOverride extends BaseOverride {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		if (player.getGameMode() == GameMode.ADVENTURE) {
			return false;
		}
		return true;
	}

	@Override
	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity, ItemStack itemInHand) {
		return clickedEntity == null || !(clickedEntity instanceof Snowman) || clickedEntity.getCustomName() == null;
	}
}

package com.playmonumenta.plugins.overrides;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;

public class BaseOverride {
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		return true;
	}
	public boolean leftClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		return true;
	}

	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		return true;
	}
	public boolean leftClickBlockInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		return true;
	}

	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity, ItemStack itemInHand) {
		return true;
	}

	public boolean physicsInteraction(Plugin plugin, Player player, Block block) {
		return true;
	}

	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		return true;
	}
	public boolean blockBreakInteraction(Plugin plugin, Player player, Block block) {
		return true;
	}
	public boolean blockExplodeInteraction(Plugin plugin, Block block) {
		return true;
	}

	public boolean blockDispenseInteraction(Plugin plugin, Block block, ItemStack dispensed) {
		return true;
	}

	public boolean blockChangeInteraction(Plugin plugin, Block block) {
		return true;
	}
}

package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BaseOverride {
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		return true;
	}

	public boolean leftClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		return true;
	}

	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item, Block block, PlayerInteractEvent event) {
		return true;
	}

	public boolean leftClickBlockInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item, Block block) {
		return true;
	}

	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity, ItemStack itemInHand) {
		return true;
	}

	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		return true;
	}

	public boolean blockBreakInteraction(Plugin plugin, Player player, Block block, BlockBreakEvent event) {
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

	public boolean playerItemConsume(Plugin plugin, Player player, PlayerItemConsumeEvent event) {
		return true;
	}

	public boolean playerRiptide(Plugin plugin, Player player, PlayerRiptideEvent event) {
		return true;
	}

	public boolean inventoryClickInteraction(Plugin plugin, Player player, ItemStack item, InventoryClickEvent event) {
		return true;
	}
}

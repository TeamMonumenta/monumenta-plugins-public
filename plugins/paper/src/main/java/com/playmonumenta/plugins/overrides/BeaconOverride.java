package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class BeaconOverride extends BaseOverride {
	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item, Block block, PlayerInteractEvent event) {
		return player.getGameMode().equals(GameMode.CREATIVE);
	}

	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		return player.getGameMode().equals(GameMode.CREATIVE);
	}

	@Override
	public boolean blockBreakInteraction(Plugin plugin, Player player, Block block, BlockBreakEvent event) {
		return player.getGameMode().equals(GameMode.CREATIVE);
	}

	@Override
	public boolean blockExplodeInteraction(Plugin plugin, Block block) {
		return false;
	}
}

package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import javax.annotation.Nullable;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class GrindstoneOverride extends BaseOverride {
	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item, Block block, PlayerInteractEvent event) {
		return (player == null || player.getGameMode() == GameMode.CREATIVE);
	}
}

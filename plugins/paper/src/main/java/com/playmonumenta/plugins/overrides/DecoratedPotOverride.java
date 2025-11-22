package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ZoneUtils;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class DecoratedPotOverride extends BaseOverride {
	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item, Block block, PlayerInteractEvent event) {
		return player.getGameMode() == GameMode.CREATIVE ||
			ZoneUtils.hasZoneProperty(block.getLocation(), ZoneUtils.ZoneProperty.CAN_DEPOSIT_INTO_POTS) ||
			(ServerProperties.getShardName().contains("plots") && !(player.getGameMode() == GameMode.ADVENTURE)); // checks for adventure mode in plots for the case of no-access guildplots
	}
}

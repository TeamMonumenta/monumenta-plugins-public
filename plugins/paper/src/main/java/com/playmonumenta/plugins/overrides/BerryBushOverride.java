package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ZoneUtils;
import javax.annotation.Nullable;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class BerryBushOverride extends BaseOverride {
	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item, Block block, PlayerInteractEvent event) {
		if (player == null) {
			return true;
		}
		Location loc = block.getLocation();
		return player.getGameMode() == GameMode.CREATIVE || !ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.NO_BERRY_BUSH_CLICKS);
	}
}

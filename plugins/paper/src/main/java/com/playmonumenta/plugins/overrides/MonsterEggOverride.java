package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.GuildPlotUtils;
import com.playmonumenta.plugins.plots.AnimalLimits;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class MonsterEggOverride extends BaseOverride {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		// Only allow creative players or players in their plots (in capital and survival) to use spawn eggs
		if ((player.getGameMode() == GameMode.CREATIVE)) {
			return true;
		}
		if (GuildPlotUtils.guildPlotUseEggsBlocked(player)) {
			return false;
		}
		if (ZoneUtils.isInPlot(player)) {
			return AnimalLimits.mayUsePossibleSpawnEgg(player.getLocation(), item);
		}
		return false;
	}

	@Override
	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity,
	                                           ItemStack itemInHand) {

		// There's an item for the Fallen Lore-Um secret quest that is a spawn egg that must be placed in an item frame
		if (clickedEntity instanceof ItemFrame) {
			return true;
		}

		// Only allow creative players or players in their plots (in capital and survival) to use spawn eggs
		if (player.getGameMode() == GameMode.CREATIVE) {
			return true;
		}

		if (GuildPlotUtils.guildPlotUseEggsBlocked(player)) {
			return false;
		}

		return false;
	}

	@Override
	public boolean blockDispenseInteraction(Plugin plugin, Block block, ItemStack dispensed) {
		if (block.getType().equals(Material.DISPENSER)) {
			if (!ServerProperties.getIsTownWorld() && InventoryUtils.testForItemWithName(dispensed, "Dummy", false)) {
				return false;
			}

			return AnimalLimits.mayUsePossibleSpawnEgg(block.getLocation(), dispensed);
		}

		return true;
	}
}

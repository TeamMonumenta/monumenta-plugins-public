package com.playmonumenta.plugins.items;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class AnvilOverride extends OverrideItem {
	final static String REPAIR_OBJECTIVE = "RepairT";

	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action,
	                                          ItemStack item, Block block) {
		if (player == null || player.getGameMode() == GameMode.CREATIVE) {
			return true;
		} else if (player.getGameMode() == GameMode.ADVENTURE) {
			return false;
		}

		if (item != null && item.getDurability() > 0 && !item.getType().isBlock()
		    && (!item.hasItemMeta() || !item.getItemMeta().hasLore()
		        || !InventoryUtils.testForItemWithLore(item, "* Irreparable *"))
		    && block.hasMetadata(Constants.ANVIL_CONFIRMATION_METAKEY)) {

			item.setDurability((short)0);
			plugin.mWorld.playSound(player.getLocation(), "block.anvil.use", 1.0f, 1.0f);
			block.removeMetadata(Constants.ANVIL_CONFIRMATION_METAKEY, plugin);
			block.setType(Material.AIR);

			int repCount = ScoreboardUtils.getScoreboardValue(player, REPAIR_OBJECTIVE);
			ScoreboardUtils.setScoreboardValue(player, REPAIR_OBJECTIVE, repCount + 1);
		} else {
			player.sendMessage(ChatColor.GOLD + "Right click the anvil with the item you want to repair");
			block.setMetadata(Constants.ANVIL_CONFIRMATION_METAKEY, new FixedMetadataValue(plugin, true));
		}
		return false;
	}
}

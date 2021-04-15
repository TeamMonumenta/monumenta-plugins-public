package com.playmonumenta.plugins.overrides;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.SinglePageGUIManager;
import com.playmonumenta.plugins.guis.singlepageguis.PebGui;
import com.playmonumenta.plugins.utils.ItemUtils;

public class WrittenBookOverride extends BaseOverride {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
			return true;
		}
		if (ItemUtils.getPlainName(item).contains("Personal Enchanted Book")) {
			if (player.hasPermission("monumenta.peb")) {
				SinglePageGUIManager.openGUI(player, new PebGui(player, null));
				return false;
			}
		}
		return true;
	}
}
package com.playmonumenta.plugins.overrides;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemUtils;

public class SignOverride extends BaseOverride {
	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		//Compile all the lines of text together and make sure it is not a leaderboard that is being clicked
		Sign sign = (Sign)block.getState();
		String display = "";

		for (String line : sign.getLines()) {
			line = line.trim();
			if (line.matches("^[-=+]*$")) {
				//When dumping signs to chat, skip decoration lines
				continue;
			}
			display += line + " ";
		}

		if (!display.toLowerCase().contains("click") && !display.toLowerCase().contains("leaderboard")) {
			player.sendMessage(display);
		}
		return item == null || !(item.hasItemMeta() && item.getItemMeta().hasLore() && ItemUtils.isDye(item.getType()));
	}
}

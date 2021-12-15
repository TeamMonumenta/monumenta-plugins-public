package com.playmonumenta.plugins.overrides;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;

import net.kyori.adventure.text.Component;

public class SignOverride extends BaseOverride {
	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item, Block block, PlayerInteractEvent event) {
		//Compile all the lines of text together and make sure it is not a leaderboard that is being clicked
		Sign sign = (Sign) block.getState();
		String display = "";

		for (Component component : sign.lines()) {
			String line = MessagingUtils.PLAIN_SERIALIZER.serialize(component).trim();
			if (line.matches("^[-=+~]*$")) {
				//When dumping signs to chat, skip decoration lines
				continue;
			}
			line = line.replaceAll("[${}]", "");
			display += line + " ";
		}

		if (!display.toLowerCase().contains("click") && !display.toLowerCase().contains("leaderboard") && !display.isEmpty()) {
			player.sendMessage(display);
		}
		return item == null || !(item.hasItemMeta() && item.getItemMeta().hasLore() && ItemUtils.isDye(item.getType()));
	}
}

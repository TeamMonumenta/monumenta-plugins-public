package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SignOverride extends BaseOverride {
	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item, Block block, PlayerInteractEvent event) {
		//Compile all the lines of text together and make sure it is not a leaderboard that is being clicked
		Sign sign = (Sign) block.getState();
		String display = "";

		boolean output = item == null || !(item.hasItemMeta() && item.getItemMeta().hasLore() && ItemUtils.isDye(item.getType()));

		for (Component component : sign.lines()) {
			if (component.clickEvent() != null) {
				return output;
			}
			String line = MessagingUtils.PLAIN_SERIALIZER.serialize(component).trim();
			if (line.matches("^[-=+~]*$")) {
				//When dumping signs to chat, skip decoration lines
				continue;
			}
			line = line.replaceAll("[${}]", "");
			if (component.hasDecoration(TextDecoration.OBFUSCATED)) {
				line = ChatColor.MAGIC + line + ChatColor.RESET;
			}
			display += line + " ";
		}

		if (!display.toLowerCase().contains("click") && !display.toLowerCase().contains("leaderboard") && !display.isEmpty()) {
			player.sendMessage(display);
		}
		return output;
	}
}

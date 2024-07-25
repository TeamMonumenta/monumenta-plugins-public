package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.itemstats.enums.Masterwork;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public class HasBadWordCommand {
	public static void register() {
		new CommandAPICommand("hasbadworditem")
			.withPermission("monumenta.command.hasbadworditem")
			.executesPlayer((player, args) -> {
				ItemStack item = player.getInventory().getItemInMainHand();
				if (ItemUtils.isNullOrAir(item)) {
					return;
				}

				if (MonumentaNetworkChatIntegration.hasBadWord(player, item, true)) {
					Location loc = player.getLocation();

					String nameDescription = ItemUtils.getPlainNameIfExists(item);
					if (nameDescription.isBlank()) {
						nameDescription = "without a name";
					} else {
						nameDescription = "named " + nameDescription;
					}

					Masterwork masterwork = ItemStatUtils.getMasterwork(item);
					String masterworkDescription;
					if (Masterwork.NONE.equals(masterwork)) {
						masterworkDescription = "";
					} else {
						masterworkDescription = " at masterwork " + masterwork.getName();
					}

					String message = "Bad word found on "
						+ player.getName()
						+ " (`/s " + ServerProperties.getShardName()
						+ "` `/world " + loc.getWorld().getName()
						+ "` `/tp @s " + loc.getBlockX() + " "
						+ loc.getBlockY() + " " + loc.getBlockZ()
						+ "`) on a " + item.getType().key()
						+ " " + nameDescription + masterworkDescription;
					MonumentaNetworkRelayIntegration.sendAuditLogSevereMessage(message);

					throw CommandAPI.failWithString("That item has a bad word");
				}
			})
			.register();
	}
}

package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.UUID;

public class ClaimRaffle {
	private static final String CONFIRMED_METAKEY = "MonumentaRaffleClaimMetadata";

	public static void register(Plugin plugin) {
		/* No-argument variant which just is the sender (if they are a player) */
		new CommandAPICommand("claimRaffle")
			.withPermission(CommandPermission.fromString("monumenta.command.claimraffle"))
			.executes((sender, args) -> {
				if (sender instanceof Player) {
					run(plugin, (Player)sender);
				} else {
					CommandAPI.fail(ChatColor.RED + "This command must be run by a player!");
				}
			})
			.register();
	}

	private static void run(Plugin plugin, Player player) throws WrapperCommandSyntaxException {
		if (player.hasMetadata(CONFIRMED_METAKEY)) {
			// This player is running this command twice, to gild an item. And they had a credit to gild with earlier
			MonumentaNetworkRelayIntegration.sendCheckRaffleEligibilityPacket(player.getUniqueId(), true, false);
			plugin.getLogger().info("Requested raffle redeem for " + player.getName());
		} else {
			// This player is running this command for the first time - send a request to bungee to query eligibility
			MonumentaNetworkRelayIntegration.sendCheckRaffleEligibilityPacket(player.getUniqueId(), false, false);
			plugin.getLogger().info("Requested raffle eligibility for " + player.getName());
		}
	}

	public static void queryResponseReceived(UUID uuid, boolean claimReward, boolean eligible) {
		Player player = Plugin.getInstance().getPlayer(uuid);
		if (player == null || !player.isOnline()) {
			if (claimReward && eligible) {
				/* This player tried to claim a reward then disappeared - send back the reward to bungee */
				MonumentaNetworkRelayIntegration.sendCheckRaffleEligibilityPacket(uuid, false, true);
			}
			return;
		}

		if (claimReward && eligible) {
			ItemStatUtils.addInfusion(player.getItemInHand(), ItemStatUtils.InfusionType.GILDED, 1, player.getUniqueId());
		} else if (!claimReward && eligible) {
			player.setMetadata(CONFIRMED_METAKEY, new FixedMetadataValue(Plugin.getInstance(), 0));
			player.sendMessage(ChatColor.GREEN + "You have won the weekly voting raffle! Congratulations!");
			player.sendMessage(ChatColor.GREEN + "The reward is to add the Gilded enchant to an item of your choice. This will give you a unique particle effect while it is anywhere in your inventory.");
			player.sendMessage(ChatColor.GREEN + "To claim this reward, run " + ChatColor.GOLD + "/claimraffle " + ChatColor.GREEN + "again with the item you wish to gild in your main hand.");
		} else {
			player.sendMessage(ChatColor.GREEN + "You have no unclaimed raffle rewards.");
		}
	}
}


package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.packets.BungeeCheckRaffleEligibilityPacket;
import com.playmonumenta.plugins.utils.CommandUtils;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;

public class ClaimRaffle {
	private static final String CONFIRMED_METAKEY = "MonumentaRaffleClaimMetadata";

	public static void register(Plugin plugin) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		/* No-argument variant which just is the sender (if they are a player) */
		CommandAPI.getInstance().register("claimRaffle",
		                                  CommandPermission.fromString("monumenta.command.claimraffle"),
		                                  arguments,
		                                  (sender, args) -> {
											  if (sender instanceof Player) {
												  run(plugin, (Player)sender);
											  } else {
												  CommandAPI.fail(ChatColor.RED + "This command must be run by a player!");
											  }
		                                  });
	}

	private static void run(Plugin plugin, Player player) throws CommandSyntaxException {
		if (player.hasMetadata(CONFIRMED_METAKEY)) {
			// This player is running this command twice, to gild an item. And they had a credit to gild with earlier
			plugin.mSocketManager.sendPacket(new BungeeCheckRaffleEligibilityPacket(player.getUniqueId(), true, false));
			plugin.getLogger().info("Requested raffle redeem for " + player.getName());
		} else {
			// This player is running this command for the first time - send a request to bungee to query eligibility
			plugin.mSocketManager.sendPacket(new BungeeCheckRaffleEligibilityPacket(player.getUniqueId(), false, false));
			plugin.getLogger().info("Requested raffle eligibility for " + player.getName());
		}
	}

	public static void queryResponseReceived(Plugin plugin, UUID uuid, boolean claim_reward, boolean eligible) {
		Player player = plugin.getPlayer(uuid);
		if (player == null || !player.isOnline()) {
			if (claim_reward && eligible) {
				/* This player tried to claim a reward then disappeared - send back the reward to bungee */
				plugin.mSocketManager.sendPacket(new BungeeCheckRaffleEligibilityPacket(player.getUniqueId(), false, true));
			}
			return;
		}

		if (claim_reward && eligible) {
			try {
				CommandUtils.enchantify(player, player, "Gilded", "Gilded by");
			} catch (CommandSyntaxException ex) {
				/* Failed to claim reward - send back the reward to bungee */
				plugin.mSocketManager.sendPacket(new BungeeCheckRaffleEligibilityPacket(player.getUniqueId(), false, true));
				player.sendMessage(ChatColor.RED + ex.getMessage());
			}
		} else if (!claim_reward && eligible) {
			player.setMetadata(CONFIRMED_METAKEY, new FixedMetadataValue(plugin, 0));
			player.sendMessage(ChatColor.GREEN + "You have won the weekly voting raffle! Congratulations!");
			player.sendMessage(ChatColor.GREEN + "The reward is to add the Gilded enchant to an item of your choice. This will give you a unique particle effect while it is anywhere in your inventory.");
			player.sendMessage(ChatColor.GREEN + "To claim this reward, run " + ChatColor.GOLD + "/claimraffle " + ChatColor.GREEN + "again with the item you wish to gild in your main hand.");
		} else {
			player.sendMessage(ChatColor.GREEN + "You have no unclaimed raffle rewards.");
		}
	}
}


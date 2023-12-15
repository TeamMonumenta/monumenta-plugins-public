package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.RemoteDataAPI;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ClaimRaffle {
	private static final String RAFFLE_WINS_UNCLAIMED = "raffleWinsUnclaimed";

	/*
	 * Set of players who have confirmed redeeming the raffle. Weak, so if the player gets garbage collected
	 * they'll automatically be removed from the map.
	 */
	private static final Set<Player> CONFIRMED_PLAYERS = Collections.newSetFromMap(new WeakHashMap<>());

	public static void register(Plugin plugin) {
		/* No-argument variant which just is the sender (if they are a player) */
		new CommandAPICommand("claimRaffle")
			.withPermission(CommandPermission.fromString("monumenta.command.claimraffle"))
			.executes((sender, args) -> {
				if (sender instanceof Player) {
					run(plugin, (Player)sender);
				} else {
					throw CommandAPI.failWithString("This command must be run by a player!");
				}
			})
			.register();
	}

	private static void run(Plugin plugin, Player player) {
		MonumentaRedisSyncAPI.runOnMainThreadWhenComplete(plugin, RemoteDataAPI.get(player.getUniqueId(), RAFFLE_WINS_UNCLAIMED), (data, ex) -> {
			if (ex != null) {
				MessagingUtils.sendError(player, "Failed to get unclaimed raffle rewards: " + ex.getMessage());
			} else {
				int amountAvailable = 0;
				if (data != null) {
					try {
						amountAvailable = Integer.parseInt(data);
					} catch (Exception e) {
						MessagingUtils.sendError(player, "Failed to parse unclaimed raffle rewards as int: " + data);
						return;
					}
				}

				if (!player.isOnline() || player.isDead()) {
					// Silently abort, the player left
					return;
				}

				if (amountAvailable <= 0) {
					player.sendMessage(Component.text("You have no unclaimed raffle rewards.", NamedTextColor.GREEN));
					return;
				}

				if (CONFIRMED_PLAYERS.add(player)) {
					player.sendMessage(Component.text("You have won the weekly voting raffle! Congratulations!", NamedTextColor.GREEN));
					player.sendMessage(Component.text("The reward is to add the Gilded enchant to an item of your choice. This will give you a unique particle effect while equipped or held.", NamedTextColor.GREEN));
					player.sendMessage(Component.text("To claim this reward, run ", NamedTextColor.GREEN).append(Component.text("/claimraffle ", NamedTextColor.GOLD)).append(Component.text("again with the item you wish to gild in your main hand.", NamedTextColor.GREEN)));
					return;
				}

				// At this point the player has at least one raffle reward AND has run this command before to confirm

				// Now try to atomically remove a raffle reward
				MonumentaRedisSyncAPI.runOnMainThreadWhenComplete(plugin, RemoteDataAPI.increment(player.getUniqueId(), RAFFLE_WINS_UNCLAIMED, -1), (resultRemaining, resultEx) -> {
					if (resultEx != null) {
						MessagingUtils.sendError(player, "Failed to decrement unclaimed raffle rewards: " + resultEx.getMessage());
					} else {
						if (resultRemaining == null) {
							MessagingUtils.sendError(player, "Somehow got null despite decrementing raffle value - maybe try again?");
							return;
						}

						if (resultRemaining < 0 || !player.isOnline() || player.isDead()) {
							// This can happen if you try to claim the rewards twice at the same time - need to abort this attempt and re-add the values
							if (player.isOnline()) {
								MessagingUtils.sendError(player, "Got negative remaining raffle rewards after redeeming - be patient and only try once at a time.");
							}
							// Put the amount redeemed back to redis
							RemoteDataAPI.increment(player.getUniqueId(), RAFFLE_WINS_UNCLAIMED, 1);
						} else if (ItemStatUtils.getInfusionLevel(player.getInventory().getItemInMainHand(), InfusionType.GILDED) > 0) {
							if (player.isOnline()) {
								MessagingUtils.sendError(player, "Items can only be gilded once.");
							}
							// Put the amount redeemed back to redis
							RemoteDataAPI.increment(player.getUniqueId(), RAFFLE_WINS_UNCLAIMED, 1);
						} else {
							ItemStatUtils.addInfusion(player.getInventory().getItemInMainHand(), InfusionType.GILDED, 1, player.getUniqueId());
							player.sendMessage(Component.text("Your item has been Gilded. Thanks for supporting the server by voting!", NamedTextColor.GREEN));
						}
					}
				});
			}
		});
	}
}

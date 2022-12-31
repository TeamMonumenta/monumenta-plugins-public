package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleSwap {

	public static final String SWAP_TAG = "DisableSwapHands";
	public static final String SWAP_INVENTORY_TAG = "DisableSwapHandsInventory";

	public static void register() {
		new CommandAPICommand("toggleswap")
			.executes((sender, args) -> {
				run(sender, SWAP_TAG, "Swapping hands");
			})
			.register();

		new CommandAPICommand("toggleinventoryswap")
			.executes((sender, args) -> {
				run(sender, SWAP_INVENTORY_TAG, "Swapping in inventory");
			})
			.register();
	}

	public static void run(CommandSender sender, String tag, String text) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);

		String message;
		if (ScoreboardUtils.toggleTag(player, tag)) {
			message = text + " has been disabled";
		} else {
			message = text + " has been enabled";
		}

		player.sendMessage(Component.text(message, NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
	}
}

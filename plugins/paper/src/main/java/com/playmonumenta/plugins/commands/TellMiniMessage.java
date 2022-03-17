package com.playmonumenta.plugins.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class TellMiniMessage {

	public static void register() {
		new CommandAPICommand("tellmini")
			.withPermission("monumenta.command.tellmini")
			.withArguments(
				new EntitySelectorArgument("recipients", EntitySelectorArgument.EntitySelector.MANY_PLAYERS),
				new GreedyStringArgument("message")
			).executes((sender, args) -> {
				@SuppressWarnings("unchecked")
				Collection<Player> recipients = (Collection<Player>) args[0];
				String message = (String) args[1];

				Component parsedMessage = MiniMessage.miniMessage().parse(message);
				for (Player recipient : recipients) {
					recipient.sendMessage(parsedMessage);
				}
			}).register();
	}

}

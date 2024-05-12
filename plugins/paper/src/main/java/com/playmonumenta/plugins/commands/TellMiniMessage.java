package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class TellMiniMessage {

	public static void register() {
		new CommandAPICommand("tellmini")
			.withPermission("monumenta.command.tellmini")
			.withArguments(
				new EntitySelectorArgument.ManyPlayers("recipients"),
				new GreedyStringArgument("message")
			).executes((sender, args) -> {
				@SuppressWarnings("unchecked")
				Collection<Player> recipients = (Collection<Player>) args.get("recipients");
				String message = args.getUnchecked("message");

				Component parsedMessage = MessagingUtils.fromMiniMessage(message);
				for (Player recipient : recipients) {
					recipient.sendMessage(parsedMessage);
				}
			}).register();
	}

}

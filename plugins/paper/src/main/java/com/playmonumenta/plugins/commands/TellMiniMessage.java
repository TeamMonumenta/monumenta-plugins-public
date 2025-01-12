package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.entity.Player;

public class TellMiniMessage {

	public static void register() {
		new CommandAPICommand("tellmini")
			.withPermission("monumenta.command.tellmini")
			.withSubcommands(
				new CommandAPICommand("title")
					.withArguments(
						new EntitySelectorArgument.ManyPlayers("recipients"),
						new GreedyStringArgument("title")
					).executes((sender, args) -> {
						@SuppressWarnings("unchecked")
						Collection<Player> recipients = (Collection<Player>) args.get("recipients");
						String title = args.getUnchecked("title");

						Component parsed = MessagingUtils.fromMiniMessage(title);
						for (Player recipient : recipients) {
							recipient.sendTitlePart(TitlePart.TITLE, parsed);
						}
					}),
				new CommandAPICommand("subtitle")
					.withArguments(
						new EntitySelectorArgument.ManyPlayers("recipients"),
						new GreedyStringArgument("subtitle")
					).executes((sender, args) -> {
						@SuppressWarnings("unchecked")
						Collection<Player> recipients = (Collection<Player>) args.get("recipients");
						String subtitle = args.getUnchecked("subtitle");

						Component parsed = MessagingUtils.fromMiniMessage(subtitle);
						for (Player recipient : recipients) {
							recipient.sendTitlePart(TitlePart.SUBTITLE, parsed);
						}
					}),
				new CommandAPICommand("actionbar")
					.withArguments(
						new EntitySelectorArgument.ManyPlayers("recipients"),
						new GreedyStringArgument("message")
					).executes((sender, args) -> {
						@SuppressWarnings("unchecked")
						Collection<Player> recipients = (Collection<Player>) args.get("recipients");
						String message = args.getUnchecked("message");

						Component parsedMessage = MessagingUtils.fromMiniMessage(message);
						for (Player recipient : recipients) {
							recipient.sendActionBar(parsedMessage);
						}
					}),
				new CommandAPICommand("msg")
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
					})
				)
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

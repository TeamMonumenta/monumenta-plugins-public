package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import io.papermc.paper.text.PaperComponents;
import java.io.IOException;
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
							try {
								recipient.sendTitlePart(TitlePart.TITLE, PaperComponents.resolveWithContext(parsed, sender, recipient, false));
							} catch (IOException ex) {
								ex.printStackTrace();
							}
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
							try {
								recipient.sendTitlePart(TitlePart.SUBTITLE, PaperComponents.resolveWithContext(parsed, sender, recipient, false));
							} catch (IOException ex) {
								ex.printStackTrace();
							}
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

						Component parsed = MessagingUtils.fromMiniMessage(message);
						for (Player recipient : recipients) {
							try {
								recipient.sendActionBar(PaperComponents.resolveWithContext(parsed, sender, recipient, false));
							} catch (IOException ex) {
								ex.printStackTrace();
							}
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

						Component parsed = MessagingUtils.fromMiniMessage(message);
						for (Player recipient : recipients) {
							try {
								recipient.sendMessage(PaperComponents.resolveWithContext(parsed, sender, recipient, false));
							} catch (IOException ex) {
								ex.printStackTrace();
							}
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

				Component parsed = MessagingUtils.fromMiniMessage(message);
				for (Player recipient : recipients) {
					try {
						recipient.sendMessage(PaperComponents.resolveWithContext(parsed, sender, recipient, false));
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}).register();
	}

}

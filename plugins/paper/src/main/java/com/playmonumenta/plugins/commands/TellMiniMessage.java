package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import io.papermc.paper.text.PaperComponents;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.entity.Player;

public class TellMiniMessage {
	private static final String PERMISSION = "monumenta.command.tellmini";
	private static final String NAME = "tellmini";

	private static CommandAPICommand generateSubcommand(String name, BiConsumer<Player, Component> send) {
		return new CommandAPICommand(name).withArguments(
			new EntitySelectorArgument.ManyPlayers("recipients"),
			new GreedyStringArgument("message").setOptional(true)
		).executes((sender, args) -> {
			Collection<Player> recipients = Objects.requireNonNull(args.getUnchecked("recipients"));
			String title = args.getUnchecked("message");
			Component parsed = title == null ? Component.empty() : MessagingUtils.fromMiniMessage(title);
			for (Player recipient : recipients) {
				try {
					send.accept(recipient, PaperComponents.resolveWithContext(parsed, sender, recipient, false));
				} catch (IOException e) {
					MMLog.warning("tellmini: failed to resolveWithContext:", e);
					return;
				}
			}
		});
	}

	public static void register() {
		new CommandAPICommand(NAME)
			.withPermission(PERMISSION)
			.withSubcommands(
				generateSubcommand("title", (r, m) -> r.sendTitlePart(TitlePart.TITLE, m)),
				generateSubcommand("subtitle", (r, m) -> r.sendTitlePart(TitlePart.SUBTITLE, m)),
				generateSubcommand("actionbar", Audience::sendActionBar),
				generateSubcommand("msg", Audience::sendMessage)
			).register();

		generateSubcommand(NAME, Audience::sendMessage)
			.withPermission(PERMISSION)
			.register();
	}
}

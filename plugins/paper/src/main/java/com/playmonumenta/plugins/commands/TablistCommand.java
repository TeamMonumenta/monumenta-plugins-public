package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.integrations.TABIntegration;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.List;

public class TablistCommand {
	public static final String COMMAND = "mtablist";

	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.tablist");
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withSubcommand(new CommandAPICommand("add").withPermission(perms)
				.withArguments(List.of(new StringArgument("name"), new StringArgument("shard")))
				.executesPlayer((sender, args) -> {
					TABIntegration.addFakePlayer(args.getUnchecked("name"), args.getUnchecked("shard"));
				})
			)
			.withSubcommand(new CommandAPICommand("remove").withPermission(perms)
			.executes((sender, args) -> {
				TABIntegration.removeFakePlayers();
			}))
			.register();
	}
}

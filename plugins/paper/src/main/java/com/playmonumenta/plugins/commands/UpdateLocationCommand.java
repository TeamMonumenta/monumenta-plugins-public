package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.network.ClientModHandler;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class UpdateLocationCommand {
	public static String COMMAND = "monumenta";

	@SuppressWarnings("unchecked")
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.updatelocation");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("UpdateLocation"));
		arguments.add(new EntitySelectorArgument.ManyPlayers("Targets"));

		arguments.add(new StringArgument("Shard"));
		arguments.add(new StringArgument("Content"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Collection<Player> targets = args.getUnchecked("Targets");

				String content = args.getUnchecked("Content");
				String shard = args.getUnchecked("Shard");
				run(targets, shard, content);
			}).register();
	}

	public static void run(Collection<Player> targets, @Nullable String shard, String content) {
		for (Player target : targets) {
			if (shard == null) {
				ClientModHandler.sendLocationPacket(target, content);
			} else {
				ClientModHandler.sendLocationPacket(target, shard, content);
			}
		}
	}
}

package com.playmonumenta.plugins.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.wrappers.NativeProxyCommandSender;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class RunWithPlaceholdersCommand {

	public static void register() {
		new CommandAPICommand("papiexecute")
			.withPermission("monumenta.command.papiexecute")
			.withArguments(
				new LiteralArgument("no-player"),
				new GreedyStringArgument("command"))
			.executes((sender, args) -> {
				String command = (String) args[0];

				run(null, command, sender);
			})
			.register();

		new CommandAPICommand("papiexecute")
			.withPermission("monumenta.command.papiexecute")
			.withArguments(
				new EntitySelectorArgument.OnePlayer("player"),
				new GreedyStringArgument("command"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				String command = (String) args[1];

				run(player, command, sender);
			})
			.register();
	}

	private static void run(@Nullable Player player, String command, CommandSender sender) {
		command = PlaceholderAPI.setPlaceholders(player, command);
		if (sender instanceof NativeProxyCommandSender proxy) { // Bukkit doesn't like this command sender type
			if (proxy.getCallee() instanceof Entity entity) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as " + entity.getUniqueId() + " at @s run " + command);
			} else {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
			}
		} else {
			Bukkit.dispatchCommand(sender, command);
		}
	}

}

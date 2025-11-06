package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.MetadataUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.CommandArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.FunctionArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TimeArgument;
import dev.jorel.commandapi.wrappers.CommandResult;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;

public class FunctionCooldownCommand {
	public static final String COMMAND = "functioncooldown";
	public static final String PERMISSION = "monumenta.command.functioncooldown";

	public static void register() {
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.executes((sender, args) -> {
				sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
				sender.sendMessage(Component.text("/functioncooldown: ", NamedTextColor.GOLD).append(Component.text("Shows this help text.", NamedTextColor.WHITE)));
				sender.sendMessage(Component.text("/functioncooldown <cooldown> <entity> <cooldown_key> function <function> [fail_function]:", NamedTextColor.GOLD));
				sender.sendMessage(Component.text("Runs the provided function as the entity, and puts the function \"on cooldown\" for the entity.", NamedTextColor.WHITE));
				sender.sendMessage(Component.text("If the function is on cooldown, it will not be run, and the optional fail function will be run.", NamedTextColor.WHITE));
				sender.sendMessage(Component.text("All /functioncooldown usages with the same \"cooldown key\" will share a cooldown. For example:", NamedTextColor.WHITE));
				sender.sendMessage(Component.text("/functioncooldown 10s @s ChooseColorCooldown function monumenta:choose_green", NamedTextColor.GREEN));
				sender.sendMessage(Component.text("/functioncooldown 10s @s ChooseColorCooldown function monumenta:choose_blue", NamedTextColor.AQUA));
				sender.sendMessage(Component.text("Running either one of the above will put both on cooldown for the entity.", NamedTextColor.WHITE));
				sender.sendMessage(Component.text("/functioncooldown <cooldown> <entity> <cooldown_key> command <command>:", NamedTextColor.GOLD));
				sender.sendMessage(Component.text("Same as above, but takes a full command instead of a function name.", NamedTextColor.WHITE));
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				new TimeArgument("cooldown"),
				new EntitySelectorArgument.OneEntity("entity"),
				new StringArgument("cooldown_key"),
				new LiteralArgument("literalnode", "function"),
				new FunctionArgument("function")
			)
			.withOptionalArguments(
				new FunctionArgument("fail_function")
			)
			.executes((sender, args) -> {
				int cooldown = args.getUnchecked("cooldown");
				Entity entity = args.getUnchecked("entity");
				String key = args.getUnchecked("cooldown_key");
				FunctionWrapper[] function = args.getUnchecked("function");
				FunctionWrapper[] failFunction = args.getUnchecked("fail_function");
				if (MetadataUtils.checkOnceInRecentTicks(Plugin.getInstance(), entity, key, cooldown)) {
					if (function != null && function.length > 0) {
						function[0].runAs(entity);
					}
				} else if (failFunction != null && failFunction.length > 0) {
					failFunction[0].runAs(entity);
				}
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				new TimeArgument("cooldown"),
				new EntitySelectorArgument.OneEntity("entity"),
				new StringArgument("cooldown_key"),
				new LiteralArgument("literalnode", "command"),
				new CommandArgument("command")
			)
			.executes((sender, args) -> {
				int cooldown = args.getUnchecked("cooldown");
				Entity entity = args.getUnchecked("entity");
				String key = args.getUnchecked("cooldown_key");
				CommandResult result = (CommandResult) args.get("command");
				if (MetadataUtils.checkOnceInRecentTicks(Plugin.getInstance(), entity, key, cooldown)) {
					if (result != null) {
						result.execute(entity);
					}
				}
			})
			.register();
	}
}

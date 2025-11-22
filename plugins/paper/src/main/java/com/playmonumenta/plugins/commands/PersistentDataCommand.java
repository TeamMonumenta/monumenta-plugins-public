package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Constants;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.NamespacedKeyArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.WorldInfo;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

public class PersistentDataCommand {

	public static void register() {
		new CommandAPICommand("persistentdata")
			.withPermission("monumenta.command.persistentdata")
			.withSubcommand(
				new CommandAPICommand("world")
					.withSubcommand(
						new CommandAPICommand("get")
							.withArguments(new LiteralArgument("current-world"))
							.withArguments(new NamespacedKeyArgument("key")
								.replaceSuggestions(ArgumentSuggestions.stringCollection(
									info -> {
										if (info.sender() instanceof Player player) {
											return suggestions(player.getWorld());
										} else {
											return Collections.emptyList();
										}
									})))
							.executesPlayer((player, args) -> {
								NamespacedKey key = args.getUnchecked("key");
								get(player, player.getWorld(), key);
							}))
					.withSubcommand(
						new CommandAPICommand("get")
							.withArguments(new StringArgument("world")
								.replaceSuggestions(ArgumentSuggestions.stringCollection(info -> Bukkit.getWorlds().stream().map(WorldInfo::getName).toList())))
							.withArguments(new NamespacedKeyArgument("key")
								.replaceSuggestions(ArgumentSuggestions.stringCollection(
									info -> suggestions(Bukkit.getWorld((String) info.previousArgs().getUnchecked("world"))))))
							.executes((sender, args) -> {
								String worldName = args.getUnchecked("world");
								World world = Bukkit.getWorld(worldName);
								if (world == null) {
									throw CommandAPI.failWithString("Unknown world " + worldName);
								}
								NamespacedKey key = args.getUnchecked("key");
								get(sender, world, key);
							}))

					.withSubcommand(
						new CommandAPICommand("set")
							.withArguments(new LiteralArgument("current-world"))
							.withArguments(new NamespacedKeyArgument("key")
								.replaceSuggestions(ArgumentSuggestions.stringCollection(
									info -> {
										if (info.sender() instanceof Player player) {
											return suggestions(player.getWorld());
										} else {
											return Collections.emptyList();
										}
									})))
							.withArguments(new MultiLiteralArgument("type", Arrays.stream(Constants.SupportedPersistentDataType.values()).map(e -> e.name().toLowerCase(Locale.ROOT)).toArray(String[]::new)))
							.withArguments(new GreedyStringArgument("value"))
							.executesPlayer((player, args) -> {
								NamespacedKey key = args.getUnchecked("key");
								String dataType = args.getUnchecked("type");
								String valueString = args.getUnchecked("value");

								set(player, player.getWorld(), key, dataType, valueString);
							}))
					.withSubcommand(
						new CommandAPICommand("set")
							.withArguments(new StringArgument("world")
								.replaceSuggestions(ArgumentSuggestions.stringCollection(info -> Bukkit.getWorlds().stream().map(WorldInfo::getName).toList())))
							.withArguments(new NamespacedKeyArgument("key")
								.replaceSuggestions(ArgumentSuggestions.stringCollection(
									info -> suggestions(Bukkit.getWorld((String) info.previousArgs().getUnchecked("world"))))))
							.withArguments(new MultiLiteralArgument("type", Constants.SupportedPersistentDataType.getLowerCaseNames()))
							.withArguments(new GreedyStringArgument("value"))
							.executes((sender, args) -> {
								String worldName = args.getUnchecked("world");
								World world = Bukkit.getWorld(worldName);
								if (world == null) {
									throw CommandAPI.failWithString("Unknown world " + worldName);
								}
								NamespacedKey key = args.getUnchecked("key");
								String dataType = args.getUnchecked("type");
								String valueString = args.getUnchecked("value");
								set(sender, world, key, dataType, valueString);
							}))
			)
			.register();
	}

	private static Collection<String> suggestions(@Nullable World world) {
		if (world == null) {
			return Collections.emptyList();
		}
		return world.getPersistentDataContainer().getKeys().stream().map(NamespacedKey::asString).toList();
	}

	private static void get(CommandSender sender, World world, NamespacedKey key) {
		if (!world.getPersistentDataContainer().has(key)) {
			sender.sendMessage(Component.text("No such data on this world!"));
			return;
		}

		for (Constants.SupportedPersistentDataType supportedType : Constants.SupportedPersistentDataType.values()) {
			if (world.getPersistentDataContainer().has(key, supportedType.mPersistentDataType)) {
				Object value = world.getPersistentDataContainer().get(key, supportedType.mPersistentDataType);
				if (value != null) {
					sender.sendMessage(Component.text("Value (" + supportedType.name().toLowerCase(Locale.ROOT) + "): " + value));
					return;
				}
			}
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void set(CommandSender sender, World world, NamespacedKey key, String dataType, String valueString) throws WrapperCommandSyntaxException {
		Constants.SupportedPersistentDataType type;
		try {
			type = Constants.SupportedPersistentDataType.valueOf(dataType.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw CommandAPI.failWithString("Invalid data type " + dataType);
		}

		try {
			Object value = type.mParser.apply(valueString);
			world.getPersistentDataContainer().set(key, (PersistentDataType) type.mPersistentDataType, value);
			sender.sendMessage(Component.text("Data updated."));
		} catch (NumberFormatException e) {
			throw CommandAPI.failWithString(valueString + " cannot be parsed as a " + type.name().toLowerCase(Locale.ROOT));
		}
	}

}

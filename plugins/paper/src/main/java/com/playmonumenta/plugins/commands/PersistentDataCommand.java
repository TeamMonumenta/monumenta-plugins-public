package com.playmonumenta.plugins.commands;

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
import java.util.function.Function;
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

	private enum SupportedPersistentDataType {
		BYTE(PersistentDataType.BYTE, Byte::parseByte),
		SHORT(PersistentDataType.SHORT, Short::parseShort),
		INTEGER(PersistentDataType.INTEGER, Integer::parseInt),
		LONG(PersistentDataType.LONG, Long::parseLong),
		FLOAT(PersistentDataType.FLOAT, Float::parseFloat),
		DOUBLE(PersistentDataType.DOUBLE, Double::parseDouble),
		STRING(PersistentDataType.STRING, s -> s),
		;

		private final PersistentDataType<?, ?> mPersistentDataType;
		private final Function<String, Object> mParser;

		SupportedPersistentDataType(PersistentDataType<?, ?> persistentDataType, Function<String, Object> parser) {
			this.mPersistentDataType = persistentDataType;
			this.mParser = parser;
		}
	}

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
								NamespacedKey key = (NamespacedKey) args[0];
								get(player, player.getWorld(), key);
							}))
					.withSubcommand(
						new CommandAPICommand("get")
							.withArguments(new StringArgument("world")
								               .replaceSuggestions(ArgumentSuggestions.stringCollection(info -> Bukkit.getWorlds().stream().map(WorldInfo::getName).toList())))
							.withArguments(new NamespacedKeyArgument("key")
								               .replaceSuggestions(ArgumentSuggestions.stringCollection(
									               info -> suggestions(Bukkit.getWorld((String) info.previousArgs()[0])))))
							.executes((sender, args) -> {
								World world = Bukkit.getWorld((String) args[0]);
								if (world == null) {
									throw CommandAPI.failWithString("Unknown world " + args[0]);
								}
								NamespacedKey key = (NamespacedKey) args[1];
								get(sender, world, key);
							}))

					.withSubcommand(
						new CommandAPICommand("set")
							.withArguments(new LiteralArgument("current-world"))
							.withArguments(new NamespacedKeyArgument("key")
								               .replaceSuggestions(ArgumentSuggestions.stringCollection(
									               info -> suggestions(Bukkit.getWorld((String) info.previousArgs()[0])))))
							.withArguments(new MultiLiteralArgument(Arrays.stream(SupportedPersistentDataType.values()).map(e -> e.name().toLowerCase(Locale.ROOT)).toArray(String[]::new)))
							.withArguments(new GreedyStringArgument("value"))
							.executesPlayer((player, args) -> {
								NamespacedKey key = (NamespacedKey) args[0];
								String dataType = (String) args[1];
								String valueString = (String) args[2];

								set(player, player.getWorld(), key, dataType, valueString);
							}))
					.withSubcommand(
						new CommandAPICommand("set")
							.withArguments(new StringArgument("world")
								               .replaceSuggestions(ArgumentSuggestions.stringCollection(info -> Bukkit.getWorlds().stream().map(WorldInfo::getName).toList())))
							.withArguments(new NamespacedKeyArgument("key")
								               .replaceSuggestions(ArgumentSuggestions.stringCollection(
									               info -> suggestions(Bukkit.getWorld((String) info.previousArgs()[0])))))
							.withArguments(new MultiLiteralArgument(Arrays.stream(SupportedPersistentDataType.values()).map(e -> e.name().toLowerCase(Locale.ROOT)).toArray(String[]::new)))
							.withArguments(new GreedyStringArgument("value"))
							.executes((sender, args) -> {
								World world = Bukkit.getWorld((String) args[0]);
								if (world == null) {
									throw CommandAPI.failWithString("Unknown world " + args[0]);
								}
								NamespacedKey key = (NamespacedKey) args[1];
								String dataType = (String) args[2];
								String valueString = (String) args[3];
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
		for (SupportedPersistentDataType supportedType : SupportedPersistentDataType.values()) {
			try {
				Object value = world.getPersistentDataContainer().get(key, supportedType.mPersistentDataType);
				if (value != null) {
					sender.sendMessage(Component.text("Value (" + supportedType.name().toLowerCase(Locale.ROOT) + "): " + value));
					return;
				}
			} catch (IllegalArgumentException e) {
				// wrong data type, ignore
			}
		}
		sender.sendMessage(Component.text("No such data on this world!"));
	}

	private static void set(CommandSender sender, World world, NamespacedKey key, String dataType, String valueString) throws WrapperCommandSyntaxException {
		SupportedPersistentDataType type;
		try {
			type = SupportedPersistentDataType.valueOf(((String) dataType).toUpperCase(Locale.ROOT));
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

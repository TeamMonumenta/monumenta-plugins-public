package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public class GlowingCommand {

	public static final String COMMAND = "glowing";
	public static final String PERMISSION = "monumenta.command.glowing";

	public static final String SCOREBOARD_OBJECTIVE = "GlowingOptions";

	/**
	 * Player options for enabled glowing effects for certain entity types.
	 * These settings are binary packed into a scoreboard value, so {@link #mPackedIndex} must not be modified once it's set.
	 * If an option is removed, do not re-use its packed index (unless the setting should carry over) or move any other indices around!
	 */
	public enum Option {
		// special option for toggling all entities on/off.
		ALL("all entities", -1) {
			@Override
			boolean appliesTo(Player player, Entity entity) {
				return true;
			}
		},

		// normal options
		OTHER_PLAYERS("other players", 1) {
			@Override
			boolean appliesTo(Player player, Entity entity) {
				return entity instanceof Player && entity != player;
			}
		},
		SELF("yourself", 6) {
			@Override
			boolean appliesTo(Player player, Entity entity) {
				return player == entity;
			}
		},
		MOBS("mobs", 2) {
			@Override
			boolean appliesTo(Player player, Entity entity) {
				return entity instanceof Mob;
			}
		},
		ELITES("elite mobs", 5) {
			@Override
			boolean appliesTo(Player player, Entity entity) {
				return EntityUtils.isElite(entity);
			}
		},
		BOSSES("bosses", 3) {
			@Override
			boolean appliesTo(Player player, Entity entity) {
				return EntityUtils.isBoss(entity);
			}
		},
		INVISIBLE("invisible entities", 4) {
			@Override
			boolean appliesTo(Player player, Entity entity) {
				return entity instanceof LivingEntity && ((LivingEntity) entity).isInvisible();
			}
		},
		ITEMS("items", 7) {
			@Override
			boolean appliesTo(Player player, Entity entity) {
				return entity instanceof Item;
			}
		},

		// a somewhat special option that matches every entity that no other option matches
		MISC("miscellaneous entities", 31) {
			@Override
			boolean appliesTo(Player player, Entity entity) {
				return Arrays.stream(Option.values())
					.noneMatch(option -> option != ALL && option != MISC && option.appliesTo(player, entity));
			}
		};

		private final String mDescription;
		private final int mPackedIndex;

		Option(String description, int packedIndex) {
			if (packedIndex >= 32) {
				throw new IllegalArgumentException();
			}
			mDescription = description;
			mPackedIndex = packedIndex;
		}

		abstract boolean appliesTo(Player player, Entity entity);

		@Override
		public String toString() {
			return mDescription;
		}
	}

	public static void register() {

		new CommandAPICommand(COMMAND)
				.withPermission(PERMISSION)
				.withArguments(
						new LiteralArgument("show-config")
				)
				.executesPlayer((player, args) -> {
					showConfig(player, true);
				})
				.register();

		// enable/disable option can take a variable amount of options as arguments
		String[] optionLiterals = Arrays.stream(Option.values()).map(o -> o.name().toLowerCase(Locale.ROOT)).toArray(String[]::new);
		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new MultiLiteralArgument("action", "enable", "disable", "toggle"));
		arguments.add(new MultiLiteralArgument("option", optionLiterals));
		for (int i = 0; i < optionLiterals.length - 2; i++) { // -1 for "all", and another -1 because listing every single option makes no sense
			if (i != 0) {
				arguments.add(new StringArgument("option" + (i + 1)).replaceSuggestions(ArgumentSuggestions.strings(info -> Arrays.stream(optionLiterals)
						.filter(o -> !o.equals(Option.ALL.name().toLowerCase(Locale.ROOT)) && !info.currentInput().contains(o))
						.toArray(String[]::new))));
			}

			new CommandAPICommand(COMMAND)
					.withPermission(PERMISSION)
					.withArguments(arguments)
					.executesPlayer((player, args) -> {
						setOptions(player, Arrays.copyOfRange(args.args(), 1, args.count()), args.getUnchecked("action"));
					})
					.register();

		}

		new CommandAPICommand(COMMAND)
				.withPermission(PERMISSION)
				.withArguments(
						new LiteralArgument("help")
				)
				.executesPlayer((player, args) -> {
					showHelp(player);
				})
				.register();

	}

	private static void showHelp(Player player) {
		player.sendMessage(Component.text("Use ", NamedTextColor.GOLD).append(Component.text("/glowing enable/disable <categories...>", NamedTextColor.WHITE)).append(Component.text(" to toggle glowing for the given categories.", NamedTextColor.GOLD)));
		player.sendMessage(Component.text("This will prevent entities of that type from glowing (i.e. having an outline) for you only.", NamedTextColor.GOLD));
		player.sendMessage(Component.text("If an entity fits into more than one category (e.g. a boss matches both 'mobs' and 'bosses'), it will glow if any of the matching options are enabled.", NamedTextColor.GOLD));
	}

	private static void setOptions(Player player, Object[] optionStrings, String operation) throws WrapperCommandSyntaxException {

		List<Option> options = new ArrayList<>();
		for (Object optionString : optionStrings) {
			try {
				options.add(Option.valueOf(((String) optionString).toUpperCase(Locale.ROOT)));
			} catch (IllegalArgumentException e) {
				throw CommandAPI.failWithString("Invalid option '" + optionString + "'");
			}
		}

		int value;
		if (options.contains(Option.ALL)) {
			if ("enable".equals(operation)) {
				value = 0;
			} else if ("disable".equals(operation)) {
				value = 0xFFFFFFFF;
			} else {
				value = ~ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_OBJECTIVE).orElse(0);
			}
		} else {
			value = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_OBJECTIVE).orElse(0);
			for (Option option : options) {
				if ("enable".equals(operation)) {
					value &= ~(1 << option.mPackedIndex);
				} else if ("disable".equals(operation)) {
					value |= 1 << option.mPackedIndex;
				} else {
					value ^= 1 << option.mPackedIndex;
				}
			}
		}
		ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_OBJECTIVE, value);

		player.sendMessage(Component.text("Glowing " + operation + "d"
			+ " for " + StringUtils.join(options, ", ") + ". Your new options are:", NamedTextColor.GOLD));
		showConfig(player, false);
		player.sendMessage(Component.text(" You may need to leave and re-enter the current area for all entities to be updated.", NamedTextColor.GRAY));

	}

	private static void showConfig(Player player, boolean withHeader) {
		if (withHeader) {
			player.sendMessage(Component.text("Your active glowing options:", NamedTextColor.GOLD));
		}
		int value = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_OBJECTIVE).orElse(0);
		List<String> enabled = new ArrayList<>();
		List<String> disabled = new ArrayList<>();
		for (Option option : Option.values()) {
			if (option == Option.ALL) {
				continue;
			}
			if (isOptionEnabled(value, option)) {
				enabled.add(option.mDescription);
			} else {
				disabled.add(option.mDescription);
			}
		}
		player.sendMessage(Component.text("Glowing ", NamedTextColor.GOLD).append(Component.text("enabled", NamedTextColor.BLUE)).append(Component.text(" for: ", NamedTextColor.GOLD))
			.append(Component.text(enabled.isEmpty() ? "no entities" : disabled.isEmpty() ? "all entities" : StringUtils.join(enabled, ", "), NamedTextColor.WHITE)));
		player.sendMessage(Component.text("Glowing ", NamedTextColor.GOLD).append(Component.text("disabled", NamedTextColor.RED)).append(Component.text(" for: ", NamedTextColor.GOLD))
			.append(Component.text(disabled.isEmpty() ? "no entities" : enabled.isEmpty() ? "all entities" : StringUtils.join(disabled, ", "), NamedTextColor.WHITE)));
	}

	private static boolean isOptionEnabled(int scoreboardValue, Option option) {
		if (option == Option.ALL) {
			return scoreboardValue == 0;
		} else {
			return (scoreboardValue & (1 << option.mPackedIndex)) == 0;
		}
	}

	public static boolean isGlowingEnabled(Player player, int scoreboardValue, Entity entity) {
		for (Option option : Option.values()) {
			if (option != Option.ALL
				    && option.appliesTo(player, entity)
				    && isOptionEnabled(scoreboardValue, option)) {
				return true;
			}
		}
		return false;
	}

}

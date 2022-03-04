package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
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
			boolean appliesTo(Entity entity) {
				return true;
			}
		},

		// normal options
		PLAYERS("players", 1) {
			@Override
			boolean appliesTo(Entity entity) {
				return entity instanceof Player;
			}
		},
		MOBS("mobs", 2) {
			@Override
			boolean appliesTo(Entity entity) {
				return entity instanceof Mob;
			}
		},
		BOSSES("bosses", 3) {
			@Override
			boolean appliesTo(Entity entity) {
				return entity.getScoreboardTags().contains("Boss");
			}
		},
		INVISIBLE("invisible entities", 4) {
			@Override
			boolean appliesTo(Entity entity) {
				return entity instanceof LivingEntity && ((LivingEntity) entity).isInvisible();
			}
		},
		EXPERIENCE_ORBS("experience orbs", 5) {
			@Override
			boolean appliesTo(Entity entity) {
				return entity instanceof ExperienceOrb;
			}
		},

		// a somewhat special option that matches every entity that no other option matches
		MISC("miscellaneous entities", 31) {
			@Override
			boolean appliesTo(Entity entity) {
				return Arrays.stream(Option.values())
						.noneMatch(option -> option != ALL && option != MISC && option.appliesTo(entity));
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

		abstract boolean appliesTo(Entity entity);

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
		List<Argument> arguments = new ArrayList<>();
		arguments.add(new MultiLiteralArgument("enable", "disable", "toggle"));
		arguments.add(new MultiLiteralArgument(optionLiterals));
		for (int i = 0; i < optionLiterals.length - 2; i++) { // -1 for "all", and another -1 because listing every single option makes no sense
			if (i != 0) {
				arguments.add(new StringArgument("option" + (i + 1)).replaceSuggestions(info -> Arrays.stream(optionLiterals)
						.filter(o -> !o.equals(Option.ALL.name().toLowerCase(Locale.ROOT)) && !info.currentInput().contains(o))
						.toArray(String[]::new)));
			}

			new CommandAPICommand(COMMAND)
					.withPermission(PERMISSION)
					.withArguments(arguments)
					.executesPlayer((player, args) -> {
						setOptions(player, (Object[]) Arrays.copyOfRange(args, 1, args.length), (String) args[0]);
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
		player.sendRawMessage(ChatColor.GOLD + "Use " + ChatColor.WHITE + "/glowing enable/disable <categories...>" + ChatColor.GOLD + " to toggle glowing for the given categories.");
		player.sendRawMessage(ChatColor.GOLD + "This will prevent entities of that type from glowing (i.e. having an outline) for you only.");
		player.sendRawMessage(ChatColor.GOLD + "If an entity fits into more than one category (e.g. a boss matches both 'mobs' and 'bosses'), it will glow if any of the matching options are enabled.");
	}

	private static void setOptions(Player player, Object[] optionStrings, String operation) throws WrapperCommandSyntaxException {

		List<Option> options = new ArrayList<>();
		for (Object optionString : optionStrings) {
			try {
				options.add(Option.valueOf(((String) optionString).toUpperCase(Locale.ROOT)));
			} catch (IllegalArgumentException e) {
				CommandAPI.fail("Invalid option '" + optionString + "'");
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

		player.sendMessage(ChatColor.GOLD + "Glowing " + operation + "d"
			                   + " for " + StringUtils.join(options, ", ") + ". Your new options are:");
		showConfig(player, false);
		player.sendMessage(ChatColor.GRAY + " You may need to leave and re-enter the current area for all entities to be updated.");

	}

	private static void showConfig(Player player, boolean withHeader) {
		if (withHeader) {
			player.sendRawMessage(ChatColor.GOLD + "Your active glowing options:");
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
		player.sendRawMessage(ChatColor.GOLD + "Glowing " + ChatColor.BLUE + "enabled" + ChatColor.GOLD + " for: " + ChatColor.RESET
				                      + (enabled.isEmpty() ? "no entities" : disabled.isEmpty() ? "all entities" : StringUtils.join(enabled, ", ")));
		player.sendRawMessage(ChatColor.GOLD + "Glowing " + ChatColor.RED + "disabled" + ChatColor.GOLD + " for: " + ChatColor.RESET
				                      + (disabled.isEmpty() ? "no entities" : enabled.isEmpty() ? "all entities" : StringUtils.join(disabled, ", ")));
	}

	private static boolean isOptionEnabled(int scoreboardValue, Option option) {
		if (option == Option.ALL) {
			return scoreboardValue == 0;
		} else {
			return (scoreboardValue & (1 << option.mPackedIndex)) == 0;
		}
	}

	public static boolean isGlowingEnabled(int scoreboardValue, Entity entity) {
		for (Option option : Option.values()) {
			if (option != Option.ALL
					&& option.appliesTo(entity)
					&& isOptionEnabled(scoreboardValue, option)) {
				return true;
			}
		}
		return false;
	}

}

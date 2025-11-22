package com.playmonumenta.plugins.utils;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class CommandUtils {

	private static final Pattern RE_ALLOWED_WITHOUT_QUOTES = Pattern.compile("[0-9A-Za-z_.+-]+");

	public static CommandSender getCallee(CommandSender sender) {
		if (sender instanceof ProxiedCommandSender) {
			return ((ProxiedCommandSender) sender).getCallee();
		}
		return sender;
	}

	/**
	 * Gets a CommandSender's location (player, command block, /execute, etc.)
	 *
	 * @return sender's location or raises an exception
	 */
	public static Location getLocation(@Nullable CommandSender sender) throws WrapperCommandSyntaxException {
		return getLocation(sender, false);
	}

	public static Location getLocation(@Nullable CommandSender sender, boolean doSubtractEntityOffset) throws WrapperCommandSyntaxException {
		if (sender == null) {
			throw CommandAPI.failWithString("sender is null!");
		} else if (sender instanceof Entity) {
			Location senderLoc = ((Entity) sender).getLocation();
			if (doSubtractEntityOffset) {
				senderLoc.subtract(0.5, 0.5, 0.5);
			}
			return senderLoc;
		} else if (sender instanceof BlockCommandSender) {
			return ((BlockCommandSender) sender).getBlock().getLocation();
		} else if (sender instanceof ProxiedCommandSender) {
			return getLocation(((ProxiedCommandSender) sender).getCallee(), doSubtractEntityOffset);
		} else {
			throw CommandAPI.failWithString("Failed to get required command sender coordinates");
		}
	}

	public static double parseDoubleFromString(@Nullable CommandSender sender, String str) throws Exception {
		double value;

		try {
			value = Float.parseFloat(str);
		} catch (Exception e) {
			if (sender != null) {
				error(sender, "Invalid parameter " + str + ". Must be a value between " + Float.MIN_VALUE + " and " + Float.MAX_VALUE);
			}
			throw new Exception(e);
		}

		return value;
	}

	public static double parseCoordFromString(@Nullable CommandSender sender,
	                                          double senderPos, String str) throws Exception {
		try {
			if (str.equals("~")) {
				return senderPos;
			} else if (str.startsWith("~")) {
				return senderPos + parseDoubleFromString(sender, str.substring(1));
			} else {
				return parseDoubleFromString(sender, str);
			}
		} catch (Exception e) {
			if (sender != null) {
				error(sender, "Failed to parse coordinate '" + str + "'");
			}
			throw new Exception(e);
		}
	}

	public static void error(CommandSender sender, String msg) {
		if ((sender instanceof Player)
			|| ((sender instanceof ProxiedCommandSender) && (((ProxiedCommandSender) sender).getCaller() instanceof Player))) {
			sender.sendMessage(Component.text(msg, NamedTextColor.RED));
		} else {
			sender.sendMessage(msg);
		}
	}

	/**
	 * Returns the sender as Player, if that sender is a player instance, or a proxied player.
	 * Fails with an error message if not executed by/as a player.
	 */
	public static Player getPlayerFromSender(CommandSender sender) throws WrapperCommandSyntaxException {
		if (sender instanceof Player) {
			return ((Player) sender);
		} else if (sender instanceof ProxiedCommandSender) {
			CommandSender callee = ((ProxiedCommandSender) sender).getCallee();
			if (callee instanceof Player) {
				return ((Player) callee);
			}
		}
		throw CommandAPI.failWithString("This command must be run by/as a player");
	}

	public static boolean requiresQuotes(String arg) {
		if (arg == null) {
			return true;
		}
		return !RE_ALLOWED_WITHOUT_QUOTES.matcher(arg).matches();
	}

	public static String quoteIfNeeded(@Nullable String arg) {
		if (arg == null) {
			return "null";
		}
		if (requiresQuotes(arg)) {
			return "\"" + arg.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
		} else {
			return arg;
		}
	}

	public static List<String> quoteIfNeeded(Collection<String> args) {
		List<String> result = new ArrayList<>();
		for (String arg : args) {
			result.add(quoteIfNeeded(arg));
		}
		return result;
	}

	public static List<String> alwaysQuote(Collection<String> args) {
		List<String> result = new ArrayList<>();
		for (String arg : args) {
			result.add(alwaysQuote(arg));
		}
		return result;
	}

	public static String alwaysQuote(@Nullable String arg) {
		if (arg == null) {
			return "null";
		}
		return "\"" + arg.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	public static void checkPerm(CommandSender sender, CommandPermission permission) throws WrapperCommandSyntaxException {
		if (!sender.hasPermission(permission.toString())) {
			throw CommandAPI.failWithString("You do not have permission for this command.");
		}
	}

	public static class ParseFailedException extends Exception {
	}

	/**
	 * A scanner to help implement custom command arguments
	 */
	public static class CommandArgumentScanner {

		final String mArg;
		final int mOffset;
		final @Nullable SuggestionsBuilder mInitialSuggestionsBuilder;
		@Nullable SuggestionsBuilder mCurrentSuggestionsBuilder;
		final @Nullable AtomicReference<SuggestionsBuilder> mSuggestionsBuilder;
		final Set<Character> mDelimiters;
		int mIndex;

		public CommandArgumentScanner(String arg, int offset, String delimiters, @Nullable AtomicReference<SuggestionsBuilder> suggestionsBuilder) {
			mArg = arg;
			mOffset = offset;
			mSuggestionsBuilder = suggestionsBuilder;
			mInitialSuggestionsBuilder = suggestionsBuilder != null ? suggestionsBuilder.get() : null;
			mDelimiters = new HashSet<>(delimiters.chars().mapToObj(i -> (char) i).toList());
		}

		public boolean hasMore() {
			return mIndex < mArg.length();
		}

		private @Nullable SuggestionsBuilder getSuggestionsBuilder() {
			return getSuggestionsBuilder(mIndex);
		}

		private @Nullable SuggestionsBuilder getSuggestionsBuilder(int index) {
			if (mSuggestionsBuilder != null) {
				if (mCurrentSuggestionsBuilder == null || mCurrentSuggestionsBuilder.getStart() != mOffset + index) {
					mCurrentSuggestionsBuilder = Objects.requireNonNull(mInitialSuggestionsBuilder).createOffset(mOffset + index);
					Objects.requireNonNull(mSuggestionsBuilder).set(mCurrentSuggestionsBuilder);
				}
				return mCurrentSuggestionsBuilder;
			} else {
				return null;
			}
		}

		private void clearSuggestions() {
			if (mSuggestionsBuilder != null) {
				mCurrentSuggestionsBuilder = null;
				mSuggestionsBuilder.set(mInitialSuggestionsBuilder);
			}
		}

		private void suggest(String suggestion) {
			SuggestionsBuilder builder = getSuggestionsBuilder();
			if (builder != null) {
				builder.suggest(suggestion);
			}
		}

		public void next(char c) throws ParseFailedException {
			if (mIndex < mArg.length() && mArg.charAt(mIndex) == c) {
				mIndex++;
				clearSuggestions();
			} else {
				if (c != ' ') {
					suggest("" + c);
				}
				throw new ParseFailedException();
			}
		}

		public boolean tryNext(char c) {
			if (mIndex < mArg.length() && mArg.charAt(mIndex) == c) {
				mIndex++;
				clearSuggestions();
				return true;
			}
			if (c != ' ') {
				suggest("" + c);
			}
			return false;
		}

		public int scanInt() throws ParseFailedException {
			return scanToken(s -> List.of("1"), s -> {
				try {
					return Integer.parseInt(s);
				} catch (NumberFormatException e) {
					return null;
				}
			});
		}

		public int scanInt(int min, int max, int suggestion) throws ParseFailedException {
			return scanToken(s -> {
				try {
					int value = Integer.parseInt(s);
					if (value < min) {
						return List.of("" + min);
					} else if (value > max) {
						return List.of("" + max);
					}
					return List.of("" + value);
				} catch (NumberFormatException e) {
					return List.of("" + suggestion);
				}
			}, s -> {
				try {
					int value = Integer.parseInt(s);
					if (value < min || value > max) {
						return null;
					}
					return value;
				} catch (NumberFormatException e) {
					return null;
				}
			});
		}

		public long scanLong() throws ParseFailedException {
			return scanToken(s -> List.of("1"), s -> {
				try {
					return Long.parseLong(s);
				} catch (NumberFormatException e) {
					return null;
				}
			});
		}

		public float scanFloat(float min, float max, float suggestion) throws ParseFailedException {
			return scanToken(s -> {
				try {
					float value = Float.parseFloat(s);
					if (value < min) {
						return List.of("" + min);
					} else if (value > max) {
						return List.of("" + max);
					}
					return List.of("" + value);
				} catch (NumberFormatException e) {
					return List.of("" + suggestion);
				}
			}, s -> {
				try {
					float value = Float.parseFloat(s);
					if (value < min || value > max) {
						return null;
					}
					return value;
				} catch (NumberFormatException e) {
					return null;
				}
			});
		}

		public <T extends Enum<T>> T scanEnum(Class<T> enumClass) throws ParseFailedException {
			return scanToken(p -> Arrays.stream(enumClass.getEnumConstants()).map(e -> e.name().toLowerCase(Locale.ROOT)).toList(), s -> {
				try {
					return Enum.valueOf(enumClass, s.toUpperCase(Locale.ROOT));
				} catch (IllegalArgumentException e) {
					return null;
				}
			});
		}

		public String scanWord() throws ParseFailedException {
			return scanToken(s -> List.of(), s -> s);
		}

		public <T> T scanToken(Function<String, List<String>> suggestedValues, Function<String, @Nullable T> parser) throws ParseFailedException {
			int start = mIndex;
			while (mIndex < mArg.length() && !mDelimiters.contains(mArg.charAt(mIndex))) {
				mIndex++;
			}
			T parsed = null;
			if (mIndex != start && (mIndex == mArg.length() || mDelimiters.contains(mArg.charAt(mIndex)))) {
				String value = mArg.substring(start, mIndex);
				parsed = parser.apply(value);
			}
			SuggestionsBuilder builder = getSuggestionsBuilder(start);
			if (builder != null) {
				for (String value : suggestedValues.apply(mArg.substring(start, mIndex))) {
					builder.suggest(value);
				}
			}
			if (parsed != null) {
				return parsed;
			}
			throw new ParseFailedException();
		}

	}

}

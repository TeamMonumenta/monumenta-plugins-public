package com.playmonumenta.plugins.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.playmonumenta.plugins.Plugin;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

public class MessagingUtils {
	public static final Gson GSON = new Gson();
	public static final MiniMessage MINIMESSAGE_ALL = MiniMessage.builder().tags(TagResolver.standard()).build();
	public static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
	public static final GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.gson();
	public static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();
	public static final String PLAIN_NAME_DATA_KEY = "plain_name";
	public static final List<NamedTextColor> NAMED_TEXT_COLORS = List.of(
		NamedTextColor.BLACK,
		NamedTextColor.DARK_BLUE,
		NamedTextColor.DARK_GREEN,
		NamedTextColor.DARK_AQUA,
		NamedTextColor.DARK_RED,
		NamedTextColor.DARK_PURPLE,
		NamedTextColor.GOLD,
		NamedTextColor.GRAY,
		NamedTextColor.DARK_GRAY,
		NamedTextColor.BLUE,
		NamedTextColor.GREEN,
		NamedTextColor.AQUA,
		NamedTextColor.RED,
		NamedTextColor.LIGHT_PURPLE,
		NamedTextColor.YELLOW,
		NamedTextColor.WHITE
	);
	// Use with greedy string arguments
	public static final ArgumentSuggestions TEXT_COLOR_SUGGESTIONS;
	// Use with string arguments that do not accept '#' without quotes
	public static final ArgumentSuggestions ESCAPED_TEXT_COLOR_SUGGESTIONS;

	static {
		List<String> textColorSuggestions = new ArrayList<>(NamedTextColor.NAMES.keys());
		textColorSuggestions.add("#0459AF");
		TEXT_COLOR_SUGGESTIONS = ArgumentSuggestions.strings(textColorSuggestions);
		List<String> escapedColorSuggestions = new ArrayList<>(NamedTextColor.NAMES.keys());
		escapedColorSuggestions.add("\"#0459AF\"");
		ESCAPED_TEXT_COLOR_SUGGESTIONS = ArgumentSuggestions.strings(escapedColorSuggestions);
	}

	public static String translatePlayerName(Player player, String message) {
		return message.replaceAll("@S", player.getName());
	}

	public static void sendActionBarMessage(Player player, String message) {
		sendActionBarMessage(player, message, NamedTextColor.YELLOW);
	}

	public static void sendActionBarMessage(Player player, String message, TextColor color) {
		message = translatePlayerName(player, message);
		TextComponent formattedMessage = LEGACY_SERIALIZER.deserialize(message)
			.color(color);
		player.sendActionBar(formattedMessage);
	}

	public static void sendAbilityTriggeredMessage(Player player, String message) {
		TextComponent formattedMessage = LEGACY_SERIALIZER.deserialize(message)
			.color(NamedTextColor.RED);
		player.sendActionBar(formattedMessage);
	}

	public static void sendRawMessage(Player player, String message) {
		message = translatePlayerName(player, message);
		message = message.replace('&', '§');
		TextComponent formattedMessage = LEGACY_SERIALIZER.deserialize(message);
		player.sendMessage(formattedMessage);
	}

	public static void sendStackTrace(Audience audience, Throwable e) {
		TextComponent formattedMessage;
		String errorMessage = e.getLocalizedMessage();
		if (errorMessage != null) {
			formattedMessage = LEGACY_SERIALIZER.deserialize(errorMessage);
		} else {
			errorMessage = "An error occurred with no message:";
			formattedMessage = Component.text("An error occurred without a set message. Hover for stack trace.");
		}
		formattedMessage = formattedMessage.color(NamedTextColor.RED);

		// Get the first 300 characters of the stacktrace and send them to the player
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String rawStackTrace = sw.toString();
		String sStackTrace = rawStackTrace.substring(0, Math.min(rawStackTrace.length(), 300));

		TextComponent textStackTrace = Component.text(sStackTrace.replace("\t", "  "), NamedTextColor.RED);
		formattedMessage = formattedMessage.hoverEvent(textStackTrace);
		audience.sendMessage(formattedMessage);

		MMLog.warning(errorMessage + "\n" + rawStackTrace);
	}

	public static void sendError(CommandSender receiver, String message) {
		receiver.sendMessage(Component.text(message).color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
	}

	/* Gets the difference between now and the specified time in a pretty string like 4h30m */
	public static String getTimeDifferencePretty(long time) {
		Duration remaining = Duration.ofSeconds(time - java.time.Instant.now().getEpochSecond());

		return remaining.toString()
			.substring(2)
			.replaceAll("(\\d[HMS])(?!$)", "$1 ")
			.toLowerCase(Locale.getDefault());
	}

	public static String plainText(Component formattedText) {
		// This is only legacy text because we have a bunch of section symbols lying around that need to be updated.
		String legacyText = PLAIN_SERIALIZER.serialize(formattedText);
		return plainFromLegacy(legacyText);
	}

	public static String plainFromLegacy(String legacyText) {
		return PLAIN_SERIALIZER.serialize(LEGACY_SERIALIZER.deserialize(legacyText));
	}

	public static int plainLengthFromMini(String mini) {
		String plain = plainText(fromMiniMessage(mini));
		// Remove any format ends
		plain = plain.replaceAll("<\\/[^\\s>]*>", "");
		return plain.length();
	}

	public static Component fromGson(String gsonText) {
		return fromGson(GSON.fromJson(gsonText, JsonElement.class));
	}

	public static Component fromGson(JsonElement gsonText) {
		return GSON_SERIALIZER.deserializeFromTree(gsonText);
	}

	public static JsonElement toGson(Component component) {
		return GSON_SERIALIZER.serializeToTree(component);
	}

	public static Component fromMiniMessage(String miniMessageText) {
		return MINIMESSAGE_ALL.deserialize(miniMessageText);
	}

	public static String toMiniMessage(Component component) {
		return MINIMESSAGE_ALL.serialize(component);
	}

	public static Component parseComponent(String json) {
		return GSON_SERIALIZER.deserialize(json);
	}

	public static String serializeComponent(Component component) {
		return GSON_SERIALIZER.serialize(component);
	}

	public static String legacyFromComponent(Component component) {
		return LEGACY_SERIALIZER.serialize(component);
	}

	public static Component addGradient(String s, String... hex) {
		StringBuilder hexes = new StringBuilder();
		for (String h : hex) {
			hexes.append(":#").append(h);
		}
		return fromMiniMessage("<gradient" + hexes + ">" + s);
	}

	public static void sendBoldTitle(Player player, @Nullable String title, @Nullable String subtitle) {
		sendBoldTitle(player, Component.text(title != null ? title : ""), Component.text(subtitle != null ? subtitle : ""));
	}

	public static void sendBoldTitle(Player player, @Nullable Component title, @Nullable Component subtitle) {
		sendTitle(player,
				  title == null ? Component.empty() : title.decorate(TextDecoration.BOLD),
				  subtitle == null ? Component.empty() : subtitle.decorate(TextDecoration.BOLD));
	}

	public static void sendTitle(Player player, @Nullable String title, @Nullable String subtitle) {
		sendTitle(player, Component.text(title != null ? title : ""), Component.text(subtitle != null ? subtitle : ""));
	}

	public static void sendTitle(Player player, Component title, Component subtitle) {
		sendTitle(player, title, subtitle, Title.DEFAULT_TIMES);
	}

	public static void sendTitle(Player player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
		sendTitle(player, title, subtitle, Title.Times.times(ticks(fadeIn), ticks(stay), ticks(fadeOut)));
	}

	public static void sendTitle(Player player, Component title, Component subtitle, Title.Times times) {
		player.showTitle(Title.title(title, subtitle, times));
	}

	public static @Nullable NamedTextColor colorByIndex(int index) {
		try {
			return NAMED_TEXT_COLORS.get(index);
		} catch (IndexOutOfBoundsException unused) {
			return null;
		}
	}

	public static @Nullable NamedTextColor colorByHexit(char hexit) {
		try {
			return colorByIndex(Integer.parseInt(String.valueOf(hexit), 16));
		} catch (NumberFormatException | IndexOutOfBoundsException unused) {
			return null;
		}
	}

	public static @Nullable TextColor colorFromString(String value) {
		if (value.startsWith("#")) {
			return TextColor.fromHexString(value);
		} else {
			return NamedTextColor.NAMES.value(value);
		}
	}

	public static void updatePlainName(Entity entity) {
		if (entity instanceof Player) {
			return;
		}
		updatePlainName(entity, entity.customName());
	}

	public static void updatePlainName(PersistentDataHolder dataHolder, @Nullable Component name) {
		if (dataHolder instanceof Player) {
			return;
		}
		NamespacedKey plainKey = new NamespacedKey(Plugin.getInstance(), PLAIN_NAME_DATA_KEY);
		PersistentDataContainer dataContainer = dataHolder.getPersistentDataContainer();
		if (name == null) {
			dataContainer.remove(plainKey);
		} else {
			String plainName = plainText(name);
			dataContainer.set(plainKey, PersistentDataType.STRING, plainName);
		}
	}

	private static Duration ticks(int t) {
		// 50 milliseconds per tick
		return Duration.ofMillis(t * 50L);
	}

	// This method exists just to sequester deprecation warnings that I believe we can't do anything about
	// It should be easy to convert any uses of this to a new non-deprecated method if it is found
	@SuppressWarnings("deprecation")
	public static void sendProxiedMessage(ProxiedPlayer player, String message, @Nullable NamedTextColor color) {
		ComponentBuilder builder = new ComponentBuilder(message);
		if (color != null) {
			builder.color(ChatColor.of(color.asHexString()));
		}
		player.sendMessage(builder.create());
	}

	public static Component concatinateComponents(List<Component> components) {
		Component output = Component.empty();
		for (int i = 0; i < components.size(); i++) {
			if (i > 0) {
				output = output.append(Component.newline());
			}
			output = output.append(components.get(i));
		}
		return output;
	}

}

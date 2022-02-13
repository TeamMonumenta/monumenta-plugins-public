package com.playmonumenta.plugins.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.playmonumenta.plugins.Plugin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.transformation.TransformationRegistry;
import net.kyori.adventure.text.minimessage.transformation.TransformationType;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;



public class MessagingUtils {
	public static final Gson GSON = new Gson();
	public static final MiniMessage MINIMESSAGE_ALL = MiniMessage.builder()
		.transformations(
			TransformationRegistry.builder().clear()
				.add(TransformationType.COLOR)
				.add(TransformationType.DECORATION)
				.add(TransformationType.HOVER_EVENT)
				.add(TransformationType.CLICK_EVENT)
				.add(TransformationType.KEYBIND)
				.add(TransformationType.TRANSLATABLE)
				.add(TransformationType.INSERTION)
				.add(TransformationType.FONT)
				.add(TransformationType.GRADIENT)
				.add(TransformationType.RAINBOW)
				.build()
		)
		.build();
	public static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
	public static final GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.gson();
	public static final PlainComponentSerializer PLAIN_SERIALIZER = PlainComponentSerializer.plain();

	public static String translatePlayerName(Player player, String message) {
		return message.replaceAll("@S", player.getName());
	}

	public static void sendActionBarMessage(Plugin plugin, Player player, String message) {
		sendActionBarMessage(plugin, player, message, NamedTextColor.YELLOW);
	}

	public static void sendActionBarMessage(Plugin plugin, Player player, String message, NamedTextColor color) {
		message = translatePlayerName(player, message);
		TextComponent formattedMessage = LEGACY_SERIALIZER.deserialize(message)
			.color(color);
		player.sendActionBar(formattedMessage);
	}

	public static void sendAbilityTriggeredMessage(Plugin plugin, Player player, String message) {
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

	public static void sendStackTrace(CommandSender sender, Throwable e) {
		TextComponent formattedMessage;
		String errorMessage = e.getLocalizedMessage();
		if (errorMessage != null) {
			formattedMessage = LEGACY_SERIALIZER.deserialize(errorMessage);
		} else {
			formattedMessage = Component.text("An error occured without a set message. Hover for stack trace.");
		}
		formattedMessage = formattedMessage.color(NamedTextColor.RED);

		// Get the first 300 characters of the stacktrace and send them to the player
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String sStackTrace = sw.toString();
		sStackTrace = sStackTrace.substring(0, Math.min(sStackTrace.length(), 300));

		TextComponent textStackTrace = Component.text(sStackTrace.replace("\t", "  "), NamedTextColor.RED);
		formattedMessage = formattedMessage.hoverEvent(textStackTrace);
		sender.sendMessage(formattedMessage);

		e.printStackTrace();
	}

	/* Gets the difference between now and the specified time in a pretty string like 4h30m */
	public static String getTimeDifferencePretty(long time) {
		Duration remaining = Duration.ofSeconds(time - java.time.Instant.now().getEpochSecond());

		return remaining.toString()
			.substring(2)
			.replaceAll("(\\d[HMS])(?!$)", "$1 ")
			.toLowerCase();
	}

	public static String plainText(Component formattedText) {
		// This is only legacy text because we have a bunch of section symbols lying around that need to be updated.
		String legacyText = PLAIN_SERIALIZER.serialize(formattedText);
		return plainFromLegacy(legacyText);
	}

	public static String plainFromLegacy(String legacyText) {
		return PLAIN_SERIALIZER.serialize(LEGACY_SERIALIZER.deserialize(legacyText));
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
		return MINIMESSAGE_ALL.parse(miniMessageText);
	}

	public static Component parseComponent(String json) {
		return GSON_SERIALIZER.deserialize(json);
	}

}

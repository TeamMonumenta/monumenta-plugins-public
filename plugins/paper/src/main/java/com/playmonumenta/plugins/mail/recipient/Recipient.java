package com.playmonumenta.plugins.mail.recipient;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.integrations.luckperms.GuildPermission;
import com.playmonumenta.plugins.mail.MailGui;
import com.playmonumenta.plugins.mail.MailGuiSettings;
import com.playmonumenta.plugins.mail.Mailbox;
import com.playmonumenta.plugins.mail.NoMailAccessException;
import com.playmonumenta.plugins.mail.recipient.RecipientCmdArgs.ArgTarget;
import com.playmonumenta.plugins.utils.GUIUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Recipient extends Comparable<Recipient> {
	// Group 1 is the type
	// Group 2 is the ID
	Pattern RE_TYPE_ID_PAIR
		= Pattern.compile("([^:]+):([^:]+)");

	static CompletableFuture<@Nullable Recipient> of(String typeIdPairStr) {
		Matcher matcher = RE_TYPE_ID_PAIR.matcher(typeIdPairStr);
		if (!matcher.matches()) {
			CompletableFuture<@Nullable Recipient> future = new CompletableFuture<>();
			future.complete(null);
			return future;
		}

		return of(matcher.group(1), matcher.group(2));
	}

	// Implementations should include a static method of the following signature:
	// public static CompletableFuture<@Nullable Recipient> ofRecipientIdString(String recipientId);
	// If the ID is invalid, log it and return None
	static CompletableFuture<@Nullable Recipient> of(String recipientTypeStr, String recipientIdStr) {
		RecipientType recipientType = RecipientType.byId(recipientTypeStr);
		switch (recipientType) {
			case PLAYER -> {
				return PlayerRecipient.ofRecipientIdString(recipientIdStr);
			}
			case GUILD -> {
				return GuildRecipient.ofRecipientIdString(recipientIdStr);
			}
			default -> {
				CompletableFuture<Recipient> future = new CompletableFuture<>();
				future.complete(null);
				return future;
			}
		}
	}

	static CompletableFuture<@Nullable Recipient> fromJson(JsonObject recipientJson) {
		String recipientTypeStr = recipientJson.getAsJsonPrimitive("type").getAsString();
		String recipientIdStr = recipientJson.getAsJsonPrimitive("id").getAsString();
		return of(recipientTypeStr, recipientIdStr);
	}

	default JsonObject toJson() {
		JsonObject result = new JsonObject();
		result.addProperty("type", recipientTypeStr());
		result.addProperty("id", recipientIdStr());
		return result;
	}

	RecipientType recipientType();

	default String recipientTypeStr() {
		return recipientType().id();
	}

	String recipientIdStr();

	Audience audience();

	default String redisKey(MailDirection mailDirection) {
		String redisSubKey = mailDirection.redisSubKey();
		if (redisSubKey == null) {
			return recipientTypeStr() + ":" + recipientIdStr();
		}
		return Mailbox.redisKeyPrefix(redisSubKey) + redisKey(MailDirection.DEFAULT);
	}

	default String allowListRedisKey() {
		return Mailbox.redisKeyPrefix("allowlist") + redisKey(MailDirection.DEFAULT);
	}

	default String blockListRedisKey() {
		return Mailbox.redisKeyPrefix("blocklist") + redisKey(MailDirection.DEFAULT);
	}

	String friendlyStr(MailDirection mailDirection);

	Component friendlyComponent(MailDirection mailDirection);

	default Component mailboxComponent(MailDirection mailDirection) {
		return Component.text(mailDirection.mailboxPrefix())
			.decoration(TextDecoration.ITALIC, false)
			.append(friendlyComponent(MailDirection.DEFAULT));
	}

	default Component mailboxIconName(MailGuiSettings mailGuiSettings) {
		return mailboxComponent(mailGuiSettings.mDirection)
			.color(NamedTextColor.GRAY);
	}

	default ItemStack mailboxIcon(MailGuiSettings mailGuiSettings) {
		Component mailboxIconName = mailboxIconName(mailGuiSettings);
		ItemStack result = GUIUtils.createBasicItem(Material.ENDER_CHEST, mailboxIconName);
		ItemMeta meta = result.getItemMeta();
		List<Component> lore = meta.lore();
		if (lore == null) {
			lore = new ArrayList<>();
		}

		lore.addAll(MailGui.guiSettingsPrompt(mailGuiSettings));

		meta.lore(lore);
		result.setItemMeta(meta);
		return result;
	}

	ItemStack icon(MailDirection mailDirection);

	/**
	 * Checks if the viewing player is/is not a member of this recipient (player, member of guild, etc.)
	 *
	 * @param viewer the player viewing this recipient's mail
	 * @return true if the viewer is neither this recipient nor a member of this recipient with mail access
	 */
	default boolean nonMemberCheck(Player viewer) {
		return nonMemberCheck(viewer, GuildPermission.MAIL);
	}

	/**
	 * Checks if the viewing player is/is not a member of this recipient (player, member of guild, etc.)
	 *
	 * @param viewer the player request this recipient's permission
	 * @return true if the viewer is neither this recipient nor a member of this recipient with access
	 */
	boolean nonMemberCheck(Player viewer, GuildPermission guildPermission);

	/**
	 * Checks if this recipient's mail is locked and unable to be interacted with
	 *
	 * @param viewer the player viewing this recipient's mail
	 * @throws NoMailAccessException with the reason why the mail is unavailable
	 */
	default void lockedCheck(Player viewer) throws NoMailAccessException {
	}

	/**
	 * How many mailboxes this recipient is allowed to send
	 *
	 * @return The number of mailboxes this recipient is allowed to send
	 */
	int sentMailboxLimit();

	/**
	 * How many mailboxes this recipient is allowed to receive
	 *
	 * @return The number of mailboxes this recipient is allowed to receive
	 */
	int receivedMailboxLimit();

	/**
	 * A list of argument helpers for working with recipients of any valid type
	 *
	 * @param target Who is this argument in relation to
	 *               - the caller, callee, or player/guild/etc specified via arguments?
	 * @param prefix Prefix for the argument specifying players/guilds
	 * @param suffix Suffix for the argument specifying players/guilds
	 * @return a list of RecipientCmdArgs, which provides command arguments and parses them for your convenience
	 */
	static List<RecipientCmdArgs> argumentVariants(ArgTarget target, String prefix, String suffix) {
		return List.of(
			new GuildRecipient.GuildRecipientCmdArgs(target, prefix, suffix),
			new PlayerRecipient.PlayerRecipientCmdArgs(target, prefix, suffix)
		);
	}

	int mailboxCompareTo(@NotNull Recipient o);

	@Override
	int compareTo(@NotNull Recipient o);
}

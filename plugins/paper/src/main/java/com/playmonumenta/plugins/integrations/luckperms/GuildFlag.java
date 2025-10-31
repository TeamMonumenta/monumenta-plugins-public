package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

public enum GuildFlag {
	OWNS_PLOT(
		"owns_plot",
		"owns_plot",
		"Owns a guild plot",
		NamedTextColor.GREEN,
		"Does not own a guild plot",
		NamedTextColor.RED
	),
	;

	public static final String GUILD_FLAG_PREFIX = "guild.flag.";
	public static final Pattern RE_GUILD_FLAG_ID = Pattern.compile("^guild\\.flag\\.([^.]+)$");

	public final String mFlagLpId;
	public final String mArgument;
	public final String mDescriptionTrue;
	public final TextColor mDescriptionColorTrue;
	public final String mDescriptionFalse;
	public final TextColor mDescriptionColorFalse;

	GuildFlag(
		String flagLpId,
		String argument,
		String descriptionTrue,
		TextColor descriptionColorTrue,
		String descriptionFalse,
		TextColor descriptionColorFalse
	) {
		mFlagLpId = flagLpId;
		mArgument = argument;
		mDescriptionTrue = descriptionTrue;
		mDescriptionColorTrue = descriptionColorTrue;
		mDescriptionFalse = descriptionFalse;
		mDescriptionColorFalse = descriptionColorFalse;
	}

	public String guildFlagPerm() {
		return GUILD_FLAG_PREFIX + mFlagLpId;
	}

	public Component description(@Nullable Group guild) {
		if (hasFlag(guild)) {
			return Component.text(mDescriptionTrue, mDescriptionColorTrue);
		} else {
			return Component.text(mDescriptionFalse, mDescriptionColorFalse);
		}
	}

	public boolean hasFlag(@Nullable Group guild) {
		Group guildRoot = LuckPermsIntegration.getGuildRoot(guild);
		if (guildRoot == null) {
			return false;
		}

		String flagPerm = guildFlagPerm();
		for (PermissionNode permissionNode : guildRoot.getNodes(NodeType.PERMISSION)) {
			if (permissionNode.getKey().equals(flagPerm)) {
				return permissionNode.getValue();
			}
		}
		return false;
	}

	public CompletableFuture<Void> setFlag(Group guild, boolean value) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		Group guildRoot = LuckPermsIntegration.getGuildRoot(guild);
		if (guildRoot == null) {
			future.completeExceptionally(new NullPointerException("Could not identify guild root"));
			return future;
		}

		String flagPerm = guildFlagPerm();
		NodeMap data = guildRoot.data();
		for (Node node : data.toCollection()) {
			if (!(node instanceof PermissionNode permissionNode)) {
				continue;
			}
			if (permissionNode.getPermission().equals(flagPerm)) {
				data.remove(permissionNode);
				break;
			}
		}

		if (value) {
			PermissionNode permissionNode = PermissionNode.builder().permission(flagPerm).value(true).build();
			data.add(permissionNode);
		}

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				LuckPermsIntegration.GM.saveGroup(guildRoot).join();
				LuckPermsIntegration.pushUpdate();
				future.complete(null);
			} catch (Exception ex) {
				future.completeExceptionally(ex);
			}
		});

		return future;
	}

	/**
	 * Gets the GuildFlag from a given permission node
	 *
	 * @param flagId The flag ID you wish to check
	 * @return The GuildFlag from a given flag ID, or null if not a GuildFlag
	 */
	public static @Nullable GuildFlag getGuildFlag(String flagId) {
		Matcher matcher = RE_GUILD_FLAG_ID.matcher(flagId);
		if (!matcher.matches()) {
			return null;
		}
		String guildFlagId = matcher.group(1);
		for (GuildFlag guildFlag : values()) {
			if (guildFlag.mFlagLpId.equals(guildFlagId)) {
				return guildFlag;
			}
		}
		return null;
	}
}

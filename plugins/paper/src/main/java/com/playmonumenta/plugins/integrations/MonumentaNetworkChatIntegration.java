package com.playmonumenta.plugins.integrations;

import com.playmonumenta.networkchat.ChannelManager;
import com.playmonumenta.networkchat.DefaultChannels;
import com.playmonumenta.networkchat.PlayerState;
import com.playmonumenta.networkchat.PlayerStateManager;
import com.playmonumenta.networkchat.RemotePlayerManager;
import com.playmonumenta.networkchat.channel.Channel;
import com.playmonumenta.networkchat.channel.ChannelGlobal;
import com.playmonumenta.networkchat.channel.interfaces.ChannelPermissionNode;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

// TODO Make an actual API to hook this into, rather than running commands
public class MonumentaNetworkChatIntegration {
	private static boolean ENABLED = false;

	public static void onEnable(Logger logger) {
		logger.info("Enabling NetworkChat integration");
		ENABLED = true;
	}

	public static void refreshPlayer(Player player) {
		if (!ENABLED) {
			return;
		}

		RemotePlayerManager.refreshLocalPlayer(player);
	}

	public static @Nullable ChannelGlobal createGuildChannel(CommandSender sender, String guildTag, String cleanGuildName) {
		if (!ENABLED) {
			return null;
		}

		ChannelGlobal channel = new ChannelGlobal(guildTag);
		channel.setChannelPermission("group." + cleanGuildName);
		try {
			ChannelManager.registerNewChannel(sender, channel);
			return channel;
		} catch (WrapperCommandSyntaxException ex) {
			sender.sendMessage(Component.text("Unable to create guild channel " + guildTag + ":"));
			MessagingUtils.sendStackTrace(sender, ex);
			return null;
		}
	}

	public static @Nullable Channel transferGuildChannel(Audience audience, String oldGuildTag, String newGuildTag, String newCleanGuildName) {
		if (!ENABLED) {
			return null;
		}

		Channel channel = getChannel(oldGuildTag);
		try {
			if (channel instanceof ChannelPermissionNode permissionNode) {
				ChannelManager.renameChannel(oldGuildTag, newGuildTag);
				permissionNode.setChannelPermission("group." + newCleanGuildName);

				saveChannel(channel);
				return channel;
			} else {
				audience.sendMessage(Component.text("Cannot change permissions for this channel", NamedTextColor.RED));
				return null;
			}
		} catch (WrapperCommandSyntaxException ex) {
			audience.sendMessage(Component.text("Could not transfer guild channel " + oldGuildTag + ": ", NamedTextColor.RED));
			MessagingUtils.sendStackTrace(audience, ex);
			return null;
		}
	}

	public static void deleteGuildChannel(Audience audience, String guildTag) {
		if (!ENABLED) {
			return;
		}

		Channel channel = getChannel(guildTag);
		if (channel == null) {
			return; // Nothing to do
		}
		try {
			ChannelManager.deleteChannel(guildTag);
		} catch (WrapperCommandSyntaxException ex) {
			audience.sendMessage(Component.text("Unable to delete guild channel " + guildTag + ":", NamedTextColor.RED));
			MessagingUtils.sendStackTrace(audience, ex);
		}
	}

	public static void changeGuildChannelColor(CommandSender sender, String guildTag, TextColor newColor) throws WrapperCommandSyntaxException {
		if (!ENABLED) {
			return;
		}

		Channel channel = getChannel(guildTag);
		if (channel == null) {
			throw CommandAPI.failWithString("Channel '" + guildTag + "' does not exist.");
		}

		channel.color(sender, newColor);
		saveChannel(channel);
	}

	public static boolean hasChannel(String channelName) {
		if (!ENABLED) {
			return false;
		}

		return ChannelManager.getChannelId(channelName) != null;
	}

	public static @Nullable Channel getChannel(String channelName) {
		if (!ENABLED) {
			return null;
		}

		return ChannelManager.getChannel(channelName);
	}

	// Note: May return a ChannelLoading if loading is still in progress
	public static @Nullable Channel loadChannel(String channelName) {
		if (!ENABLED) {
			return null;
		}

		UUID channelId = ChannelManager.getChannelId(channelName);
		if (channelId == null) {
			return null;
		}

		return ChannelManager.loadChannel(channelId);
	}

	public static void unloadChannel(Channel channel) {
		ChannelManager.unloadChannel(channel);
	}

	public static void saveChannel(Channel channel) {
		if (!ENABLED) {
			return;
		}

		ChannelManager.saveChannel(channel);
	}

	public static void setPlayerDefaultGuildChat(Player player, Channel channel) {
		if (!ENABLED) {
			return;
		}

		PlayerState playerState = PlayerStateManager.getPlayerState(player);
		if (playerState == null) {
			return;
		}

		DefaultChannels defaultChannels = playerState.defaultChannels();
		defaultChannels.setDefaultId(DefaultChannels.GUILD_CHANNEL, channel.getUniqueId());
	}

	public static void setPlayerDefaultGuildChat(UUID playerId, Channel channel) {
		if (!ENABLED) {
			return;
		}

		PlayerState playerState = PlayerStateManager.getPlayerState(playerId);
		if (playerState == null) {
			return;
		}

		DefaultChannels defaultChannels = playerState.defaultChannels();
		defaultChannels.setDefaultId(DefaultChannels.GUILD_CHANNEL, channel.getUniqueId());
	}

	public static void unsetPlayerDefaultGuildChat(Player player) {
		if (!ENABLED) {
			return;
		}

		PlayerState playerState = PlayerStateManager.getPlayerState(player);
		if (playerState == null) {
			return;
		}

		DefaultChannels defaultChannels = playerState.defaultChannels();
		defaultChannels.setDefaultId(DefaultChannels.GUILD_CHANNEL, null);
	}
}

package com.playmonumenta.plugins.integrations;

import com.playmonumenta.networkchat.ChannelManager;
import com.playmonumenta.networkchat.ChatFilter;
import com.playmonumenta.networkchat.DefaultChannels;
import com.playmonumenta.networkchat.NetworkChatPlugin;
import com.playmonumenta.networkchat.PlayerState;
import com.playmonumenta.networkchat.PlayerStateManager;
import com.playmonumenta.networkchat.RemotePlayerManager;
import com.playmonumenta.networkchat.channel.Channel;
import com.playmonumenta.networkchat.channel.ChannelGlobal;
import com.playmonumenta.networkchat.channel.interfaces.ChannelPermissionNode;
import com.playmonumenta.networkchat.channel.property.ChannelSettings;
import com.playmonumenta.networkchat.inlinereplacements.InlineReplacement;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.classes.PlayerSpec;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.Nullable;

public class MonumentaNetworkChatIntegration {
	private static class AbilitiesHover extends InlineReplacement {
		@RegExp
		private static final String ABILITIES_REGEX = "<abilities>";

		public AbilitiesHover() {
			super("Abilities Hover", "(?<=^|[^\\\\])<(abilities)>", "abilitieshover");

			addHandler(ABILITIES_REGEX, sender -> {
				if (!(sender instanceof Player player)) {
					return Component.text("<abilities>");
				}

				PlayerClass playerClass = new MonumentaClasses().getClassById(AbilityUtils.getClassNum(player));
				PlayerSpec playerSpec = playerClass == null ? null : playerClass.getSpecById(AbilityUtils.getSpecNum(player));
				StringBuilder lore = new StringBuilder();

				lore.append(playerClass == null ? "No Class" : playerClass.mClassName + " (" + (playerSpec == null ? "No Specialization" : playerSpec.mSpecName) + ")");
				if (playerClass != null) {
					(playerSpec != null ? Stream.concat(playerClass.mAbilities.stream(), playerSpec.mAbilities.stream()) : playerClass.mAbilities.stream()).forEach(abilityInfo -> {
						if (abilityInfo.getScoreboard() == null) {
							return;
						}

						var score = ScoreboardUtils.getScoreboardValue(player, abilityInfo.getScoreboard());
						if (score > 0) {
							lore.append("\n").append(abilityInfo.getDisplayName()).append(": ").append(score > 2 ? score - 2 + "*" : score);
						}
					});
				}

				return Component.text("ABILITIES").decoration(TextDecoration.BOLD, true).hoverEvent(Component.text(lore.toString()));
			});
		}
	}

	private static class CharmsHover extends InlineReplacement {
		@RegExp
		private static final String CHARMS_REGEX = "<charms>";
		private final CharmManager.CharmType mCharmType = CharmManager.CharmType.NORMAL;
		private static int charmPowerUsed = 0;

		public CharmsHover() {
			super("Charms Hover", "(?<=^|[^\\\\])<(charms)>", "charmshover");

			addHandler(CHARMS_REGEX, sender -> {
				if (!(sender instanceof Player player)) {
					return Component.text("<charms>");
				}
				charmPowerUsed = 0;
				List<ItemStack> charms = mCharmType.mPlayerCharms.get(player.getUniqueId());
				List<Component> lore = new ArrayList<>();

				if (charms == null || charms.size() == 0) {
					return Component.text("<charms>");
				} else {
					for (ItemStack charm : charms) {
						lore.add(charm.displayName().decoration(TextDecoration.ITALIC, false));
						charmPowerUsed += ItemStatUtils.getCharmPower(charm);
					}
				}

				ItemStack item = new ItemStack(Material.PAPER);
				ItemMeta meta = item.getItemMeta();
				meta.displayName(Component.text(player.getName() + "'s Charms (" + charmPowerUsed + "/" + mCharmType.getTotalCharmPower(player) + " Charm Power used)", NamedTextColor.WHITE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
				meta.lore(lore);
				item.setItemMeta(meta);

				return Component.text("CHARMS").decoration(TextDecoration.BOLD, true).hoverEvent(item);
			});
		}
	}

	private static boolean ENABLED = false;

	public static void onEnable(Logger logger) {
		logger.info("Enabling NetworkChat integration");
		ENABLED = true;

		// register replacement for <abilities> and <charms>
		NetworkChatPlugin.getReplacementsManagerInstance().mReplacements.add(new AbilitiesHover());
		NetworkChatPlugin.getReplacementsManagerInstance().mReplacements.add(new CharmsHover());
	}

	public static void refreshPlayer(Player player) {
		if (!ENABLED) {
			return;
		}

		RemotePlayerManager.refreshLocalPlayer(player);
	}

	public static @Nullable ChannelGlobal createGuildChannel(CommandSender sender, String guildTag, String permission) {
		if (!ENABLED) {
			return null;
		}

		ChannelGlobal channel = new ChannelGlobal(guildTag);
		channel.setChannelPermission(permission);
		try {
			ChannelManager.registerNewChannel(sender, channel);
			return channel;
		} catch (WrapperCommandSyntaxException ex) {
			sender.sendMessage(Component.text("Unable to create guild channel " + guildTag + ":"));
			MessagingUtils.sendStackTrace(sender, ex);
			return null;
		}
	}

	public static @Nullable Channel transferGuildChannel(Audience audience, String oldGuildTag, String newGuildTag, String permission) {
		if (!ENABLED) {
			return null;
		}

		Channel channel = getChannel(oldGuildTag);
		try {
			if (channel instanceof ChannelPermissionNode permissionNode) {
				ChannelManager.renameChannel(oldGuildTag, newGuildTag);
				permissionNode.setChannelPermission(permission);

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

	public static void setChannelPermission(Channel channel, String permission) {
		if (channel instanceof ChannelPermissionNode permissionNode) {
			permissionNode.setChannelPermission(permission);
		}
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

	public static void setPlayerChannelNotifications(Player player, Channel channel, boolean notificationsEnabled) {
		if (!ENABLED) {
			return;
		}

		PlayerState playerState = PlayerStateManager.getPlayerState(player);
		if (playerState == null) {
			return;
		}

		ChannelSettings channelSettings = playerState.channelSettings(channel);
		channelSettings.messagesPlaySound(notificationsEnabled);
	}

	public static boolean hasBadWord(CommandSender sender, String text) {
		return hasBadWord(sender, Component.text(text));
	}

	public static boolean hasBadWord(CommandSender sender, Component text) {
		if (!ENABLED) {
			return false;
		}

		ChatFilter chatFilter = NetworkChatPlugin.globalBadWordFilter();
		return chatFilter.hasBadWord(sender, text);
	}
}

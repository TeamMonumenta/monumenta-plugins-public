package com.playmonumenta.plugins.integrations;

import com.destroystokyo.paper.profile.PlayerProfile;
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
import com.playmonumenta.plugins.utils.ItemUtils;
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
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
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

				if (charms == null || charms.isEmpty()) {
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

	public static void setPauseChat(Player player, boolean pause) {
		if (!ENABLED) {
			return;
		}

		PlayerState playerState = PlayerStateManager.getPlayerState(player);
		if (playerState == null) {
			return;
		}

		if (pause) {
			playerState.pauseChat();
		} else {
			playerState.unpauseChat();
		}
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

	public static boolean hasBadWord(CommandSender sender, @Nullable ItemStack item, boolean recursive) {
		if (ItemUtils.isNullOrAir(item) || !item.hasItemMeta()) {
			return false;
		}

		if (hasBadWord(sender, ItemStatUtils.getPlayerCustomName(item))) {
			return true;
		}

		ItemMeta meta = item.getItemMeta();

		if (hasBadWord(sender, meta.displayName())) {
			return true;
		}

		List<Component> lore = meta.lore();
		if (lore != null) {
			for (Component loreLine : lore) {
				if (hasBadWord(sender, loreLine)) {
					return true;
				}
			}
		}

		if (
			meta instanceof BlockStateMeta blockStateMeta
				&& blockStateMeta.hasBlockState()
		) {
			BlockState blockState = blockStateMeta.getBlockState();

			if (recursive && blockState instanceof BlockInventoryHolder inventoryHolder) {
				Inventory inventory = inventoryHolder.getInventory();

				for (ItemStack content : inventory) {
					if (hasBadWord(sender, content, true)) {
						return true;
					}
				}
			}

			if (blockState instanceof Sign sign) {
				for (Component line : sign.lines()) {
					if (hasBadWord(sender, line)) {
						return true;
					}
				}
			}
		}

		if (meta instanceof BookMeta bookMeta) {
			if (hasBadWord(sender, bookMeta.title())) {
				return true;
			}

			if (hasBadWord(sender, bookMeta.author())) {
				return true;
			}

			// NOTE: pageNumber is 1-indexed, not 0-indexed - it should go from 1 to pageCount inclusive!
			for (int pageNumber = 1; pageNumber <= bookMeta.getPageCount(); pageNumber++) {
				Component page = bookMeta.page(pageNumber);
				if (hasBadWord(sender, page)) {
					return true;
				}
			}
		}

		if (recursive && meta instanceof CrossbowMeta crossbowMeta) {
			for (ItemStack subItem : crossbowMeta.getChargedProjectiles()) {
				if (hasBadWord(sender, subItem, true)) {
					return true;
				}
			}
		}

		if (meta instanceof SkullMeta skullMeta) {
			PlayerProfile playerProfile = skullMeta.getPlayerProfile();
			if (playerProfile != null) {
				String playerName = playerProfile.getName();
				if (hasBadWord(sender, playerName)) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean hasBadWord(CommandSender sender, @Nullable String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		return hasBadWord(sender, Component.text(text));
	}

	public static boolean hasBadWord(CommandSender sender, @Nullable Component text) {
		if (!ENABLED) {
			return false;
		}

		if (text == null) {
			return false;
		}

		text = text.replaceText(
			TextReplacementConfig.builder()
				.matchLiteral("\n")
				.replacement(" \\n ")
				.build()
		);

		ChatFilter chatFilter = NetworkChatPlugin.globalBadWordFilter();
		return chatFilter.hasBadWord(sender, text);
	}
}

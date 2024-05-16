package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.networkchat.channel.Channel;
import com.playmonumenta.networkchat.channel.ChannelGlobal;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.integrations.luckperms.listeners.LPArguments;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.redissync.RBoardAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;

import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_MEMBER_GUILD_NAME_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_MEMBER_HOVER_PREFIX_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_MEMBER_ROOT_COLOR_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_MEMBER_ROOT_ID_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_MEMBER_ROOT_PLAIN_TAG_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_MEMBER_ROOT_PLOT_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_ROOT_COLOR_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_ROOT_ID_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_ROOT_NAME_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_ROOT_PLAIN_TAG_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_ROOT_PLOT_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_ROOT_TAG_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_TP_MK;

public class UpgradeGuild {
	private static final long LEADERBOARD_PAGE_SIZE = 128L;

	public static void register(Plugin plugin) {
		// guild mod upgrade <guild name>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.upgradeguild");

		new CommandAPICommand("guild")
			.withArguments(new MultiLiteralArgument("mod"))
			.withArguments(new MultiLiteralArgument("upgrade"))
			.withArguments(new TextArgument("guild id")
				.replaceSuggestions(LPArguments.GROUP_SUGGESTIONS))
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, perms);
				String guildName = (String) args[args.length - 1];
				run(plugin, sender, guildName);
			})
			.register();
	}

	public static void run(Plugin plugin, CommandSender sender, String oldGuildId) {

		if (ServerProperties.getShardName().contains("build")) {
			sender.sendMessage(Component.text("This command cannot be run on the build shard.", NamedTextColor.RED));
			return;
		}

		if (!(sender instanceof Player senderPlayer)) {
			sender.sendMessage(Component.text("Guilds must be upgraded by a player", NamedTextColor.RED));
			return;
		}

		ItemStack guildBannerTemplate = senderPlayer.getInventory().getItemInMainHand();
		if (!ItemUtils.isBanner(guildBannerTemplate)) {
			String listMembersCommand = "/listgroupmembers " + CommandUtils.quoteIfNeeded(oldGuildId);
			senderPlayer.sendMessage(Component.text(
					"To upgrade a guild, you must hold their banner. Click to list guild members.",
					NamedTextColor.RED)
				.hoverEvent(Component.text(listMembersCommand))
				.insertion(listMembersCommand)
				.clickEvent(ClickEvent.runCommand(listMembersCommand)));
			return;
		}
		ItemMeta templateMeta = guildBannerTemplate.getItemMeta();

		final ItemStack guildBanner = new ItemStack(guildBannerTemplate.getType());
		ItemMeta meta = guildBanner.getItemMeta();
		if (templateMeta instanceof BannerMeta templateBannerMeta
			&& meta instanceof BannerMeta bannerMeta) {
			bannerMeta.setPatterns(templateBannerMeta.getPatterns());
		}
		guildBanner.setItemMeta(meta);

		// Guild name sanitization for command usage
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				Bukkit.getScheduler().runTask(plugin, ()
					-> sender.sendMessage(Component.text("Verifying group exists and is not upgraded...", NamedTextColor.GOLD)));
				Group oldGuildGroup = LuckPermsIntegration.GM.loadGroup(oldGuildId).join().orElse(null);
				if (oldGuildGroup == null) {
					Bukkit.getScheduler().runTask(plugin, ()
						-> sender.sendMessage(Component.text("Guild not found", NamedTextColor.RED)));
					return;
				}
				if (LuckPermsIntegration.isModern(oldGuildGroup)) {
					// Note: this is a member group, not a root group; but, nothing to do here
					Bukkit.getScheduler().runTask(plugin, ()
						-> sender.sendMessage(Component.text("Guild already upgraded", NamedTextColor.GREEN)));
					return;
				}

				Bukkit.getScheduler().runTask(plugin, ()
					-> sender.sendMessage(Component.text("Getting group values...", NamedTextColor.GOLD)));
				PrefixNode prefixNode = null;
				MetaNode hoverPrefixNode = null;
				MetaNode guildNameNode = null;
				MetaNode guildTpNode = null;

				String guildName = null;
				TextColor guildColor = null;
				String guildTag = null;
				for (Node node : oldGuildGroup.getNodes()) {
					if (node instanceof PrefixNode thePrefixNode) {
						prefixNode = thePrefixNode;
						String prefixTag = prefixNode.getMetaValue();
						if (prefixTag.length() < 7 || !prefixTag.startsWith("#")) {
							Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(Component.text(
								"Guild tag does not follow expected format of \"#0369AF[TAG]\": \""
									+ prefixTag + "\"", NamedTextColor.RED)));
							return;
						}
						guildTag = prefixTag.substring(7);
						String guildColorString = prefixTag.substring(0, 7);
						guildColor = TextColor.fromHexString(guildColorString);
					} else if (node instanceof MetaNode metaNode) {
						if (metaNode.getMetaKey().equals(GUILD_MEMBER_HOVER_PREFIX_MK)) {
							hoverPrefixNode = metaNode;
						} else if (metaNode.getMetaKey().equals(GUILD_MEMBER_GUILD_NAME_MK)) {
							guildNameNode = metaNode;
							guildName = metaNode.getMetaValue();
						} else if (metaNode.getMetaKey().equals(GUILD_TP_MK)) {
							guildTpNode = metaNode;
						}
					}
				}
				if (hoverPrefixNode == null) {
					Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(Component.text(
						"Could not get guild hover prefix node.", NamedTextColor.RED)));
					return;
				}
				if (guildTag == null) {
					Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(Component.text(
						"Could not get guild plain tag.", NamedTextColor.RED)));
					return;
				}
				if (guildColor == null) {
					Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(Component.text(
						"Could not get guild color.", NamedTextColor.RED)));
					return;
				}
				if (guildName == null) {
					Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(Component.text(
						"Could not get guild name.", NamedTextColor.RED)));
					return;
				}
				if (guildTag.startsWith("[") && guildTag.endsWith("]")) {
					guildTag = guildTag.substring(1, guildTag.length() - 1);
				}
				String guildTagPrefix = guildColor.asHexString() + "[" + guildTag + "]";

				Bukkit.getScheduler().runTask(plugin, ()
					-> sender.sendMessage(Component.text("Verifying new guild groups do not exist", NamedTextColor.GOLD)));
				String guildRootGroupId = LuckPermsIntegration.getGuildId(guildTag);
				String guildGuestGroupId = GuildAccessLevel.GUEST.groupNameFromRoot(guildRootGroupId);
				String guildMemberGroupId = GuildAccessLevel.MEMBER.groupNameFromRoot(guildRootGroupId);
				String guildManagerGroupId = GuildAccessLevel.MANAGER.groupNameFromRoot(guildRootGroupId);
				String guildFounderGroupId = GuildAccessLevel.FOUNDER.groupNameFromRoot(guildRootGroupId);

				String guildGuestInviteGroupId = GuildInviteLevel.GUEST_INVITE.groupNameFromRoot(guildRootGroupId);
				String guildMemberInviteGroupId = GuildInviteLevel.MEMBER_INVITE.groupNameFromRoot(guildRootGroupId);

				for (String groupId : List.of(
					guildRootGroupId,
					guildGuestGroupId,
					guildMemberGroupId,
					guildManagerGroupId,
					guildFounderGroupId,
					guildGuestInviteGroupId,
					guildMemberInviteGroupId
				)) {
					if (LuckPermsIntegration.GM.loadGroup(groupId).join().isPresent()) {
						Bukkit.getScheduler().runTask(plugin, ()
							-> sender.sendMessage(Component.text(
							"Cannot upgrade due to existing LP group:" + groupId, NamedTextColor.RED)));
						return;
					}
				}

				Bukkit.getScheduler().runTask(plugin, ()
					-> sender.sendMessage(Component.text("Creating groups...", NamedTextColor.GOLD)));
				Group guildRootGroup = LuckPermsIntegration.GM.createAndLoadGroup(guildRootGroupId).join();
				InheritanceNode guildRootInheritenceNode = InheritanceNode.builder(guildRootGroup).build();

				Group guildGuestInviteGroup = LuckPermsIntegration.GM.createAndLoadGroup(guildGuestInviteGroupId).join();
				NodeMap guildGuestInviteGroupData = guildGuestInviteGroup.data();
				guildGuestInviteGroupData.add(InheritanceNode.builder(guildRootGroup).build());
				LuckPermsIntegration.GM.saveGroup(guildGuestInviteGroup).join();

				Group guildMemberInviteGroup = LuckPermsIntegration.GM.createAndLoadGroup(guildMemberInviteGroupId).join();
				NodeMap guildMemberInviteGroupData = guildMemberInviteGroup.data();
				guildMemberInviteGroupData.add(InheritanceNode.builder(guildGuestInviteGroup).build());
				LuckPermsIntegration.GM.saveGroup(guildMemberInviteGroup).join();

				Group guildGuestGroup = LuckPermsIntegration.GM.createAndLoadGroup(guildGuestGroupId).join();
				NodeMap guildGuestGroupData = guildGuestGroup.data();
				guildGuestGroupData.add(guildRootInheritenceNode);
				if (guildTpNode != null) {
					guildGuestGroupData.add(guildTpNode);
				}
				LuckPermsIntegration.GM.saveGroup(guildGuestGroup).join();

				Group guildMemberGroup = LuckPermsIntegration.GM.createAndLoadGroup(guildMemberGroupId).join();
				NodeMap guildMemberGroupData = guildMemberGroup.data();
				guildMemberGroupData.add(InheritanceNode.builder(guildGuestGroup).build());
				guildMemberGroupData.add(MetaNode.builder(GUILD_MEMBER_ROOT_ID_MK, guildRootGroupId).build());
				guildMemberGroupData.add(MetaNode.builder(GUILD_MEMBER_ROOT_COLOR_MK, guildColor.asHexString()).build());
				guildMemberGroupData.add(MetaNode.builder(GUILD_MEMBER_ROOT_PLAIN_TAG_MK, guildTag).build());
				guildMemberGroupData.add(PrefixNode.builder(guildTagPrefix, 1).build());
				guildMemberGroupData.add(MetaNode.builder(GUILD_MEMBER_HOVER_PREFIX_MK, guildName).build());
				guildMemberGroupData.add(MetaNode.builder(GUILD_MEMBER_GUILD_NAME_MK, guildName).build());
				LuckPermsIntegration.GM.saveGroup(guildMemberGroup).join();
				InheritanceNode guildMemberInheritenceNode = InheritanceNode.builder(guildMemberGroup).build();

				Group guildManagerGroup = LuckPermsIntegration.GM.createAndLoadGroup(guildManagerGroupId).join();
				NodeMap guildManagerGroupData = guildManagerGroup.data();
				guildManagerGroupData.add(guildMemberInheritenceNode);
				LuckPermsIntegration.GM.saveGroup(guildManagerGroup).join();

				Group guildFounderGroup = LuckPermsIntegration.GM.createAndLoadGroup(guildFounderGroupId).join();
				NodeMap guildFounderGroupData = guildFounderGroup.data();
				guildFounderGroupData.add(InheritanceNode.builder(guildManagerGroup).build());
				LuckPermsIntegration.GM.saveGroup(guildFounderGroup).join();
				InheritanceNode guildFounderInheritenceNode = InheritanceNode.builder(guildFounderGroup).build();

				Bukkit.getScheduler().runTask(plugin, ()
					-> sender.sendMessage(Component.text("Getting all guild founders...", NamedTextColor.GOLD)));
				Set<UUID> allGuildFounders = new HashSet<>();
				long leaderboardStart = 0L;
				while (true) {
					Map<String, Integer> leaderboardSection
						= MonumentaRedisSyncIntegration.getLeaderboard("Founder",
						leaderboardStart,
						leaderboardStart + LEADERBOARD_PAGE_SIZE - 1,
						false).join();
					leaderboardStart += LEADERBOARD_PAGE_SIZE;
					if (leaderboardSection.isEmpty()) {
						Bukkit.getScheduler().runTask(plugin, ()
							-> sender.sendMessage(Component.text("No more founders discovered.", NamedTextColor.GOLD)));
						break;
					}

					boolean foundZero = false;
					for (Map.Entry<String, Integer> leaderboardEntry : leaderboardSection.entrySet()) {
						int playerScore = leaderboardEntry.getValue();
						if (playerScore == 0) {
							if (!foundZero) {
								foundZero = true;
								Bukkit.getScheduler().runTask(plugin, ()
									-> sender.sendMessage(Component.text("Found the first non-founder, ending early.", NamedTextColor.GOLD)));
							}
							continue;
						}

						String playerName = leaderboardEntry.getKey();
						UUID playerUuid = MonumentaRedisSyncIntegration.cachedNameToUuid(playerName);
						if (playerUuid == null) {
							Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(Component.text(
								"Could not get founder UUID: " + playerName, NamedTextColor.RED)));
							return;
						}
						allGuildFounders.add(playerUuid);
					}
					if (foundZero) {
						break;
					}
				}
				Bukkit.getScheduler().runTask(plugin, ()
					-> sender.sendMessage(Component.text("Found " + allGuildFounders.size()
					+ " founders in all guilds", NamedTextColor.GOLD)));

				Bukkit.getScheduler().runTask(plugin, ()
					-> sender.sendMessage(Component.text("Updating guild members:", NamedTextColor.GOLD)));
				InheritanceNode oldGuildInheritenceNode = InheritanceNode.builder(oldGuildGroup).build();
				for (UUID memberId : LuckPermsIntegration.getGroupMembers(oldGuildInheritenceNode).join()) {
					User user = LuckPermsIntegration.loadUser(memberId).join();
					String friendlyName = user.getFriendlyName();
					NodeMap userNodes = user.data();
					if (allGuildFounders.contains(memberId)) {
						userNodes.add(guildFounderInheritenceNode);
						Bukkit.getScheduler().runTask(plugin, ()
							-> sender.sendMessage(Component.text("- Updated founder " + friendlyName,
							NamedTextColor.GOLD)));
					} else {
						userNodes.add(guildMemberInheritenceNode);
						Bukkit.getScheduler().runTask(plugin, ()
							-> sender.sendMessage(Component.text("- Updated member " + friendlyName,
							NamedTextColor.GOLD)));
					}
					userNodes.remove(oldGuildInheritenceNode);
					LuckPermsIntegration.UM.saveUser(user).join();
				}

				// Get guild number
				long guildPlot;
				try {
					guildPlot = RBoardAPI.add("$Last", "GuildPlot", 1).join();
				} catch (Throwable throwable) {
					String finalGuildName = guildName;
					Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(Component.text(
						"Failed to get new guild number for '" + finalGuildName + "'!", NamedTextColor.RED)));
					return;
				}

				Bukkit.getScheduler().runTask(plugin, ()
					-> sender.sendMessage(Component.text("Assigning member plot ID " + guildPlot, NamedTextColor.GOLD)));
				guildMemberGroupData.add(MetaNode.builder(GUILD_MEMBER_ROOT_PLOT_MK, String.valueOf(guildPlot)).build());
				LuckPermsIntegration.GM.saveGroup(guildMemberGroup).join();

				Bukkit.getScheduler().runTask(plugin, ()
					-> sender.sendMessage(Component.text("Finalizing root guild group", NamedTextColor.GOLD)));
				NodeMap guildRootGroupData = guildRootGroup.data();
				guildRootGroupData.add(MetaNode.builder(GUILD_ROOT_ID_MK, guildRootGroupId).build());
				guildRootGroupData.add(MetaNode.builder(GUILD_ROOT_PLOT_MK, String.valueOf(guildPlot)).build());
				guildRootGroupData.add(MetaNode.builder(GUILD_ROOT_COLOR_MK, guildColor.asHexString()).build());
				guildRootGroupData.add(MetaNode.builder(GUILD_ROOT_PLAIN_TAG_MK, guildTag).build());
				guildRootGroupData.add(MetaNode.builder(GUILD_ROOT_TAG_MK, guildTagPrefix).build());
				guildRootGroupData.add(MetaNode.builder(GUILD_ROOT_NAME_MK, guildName).build());
				guildRootGroupData.remove(prefixNode);
				guildRootGroupData.remove(hoverPrefixNode);
				guildRootGroupData.remove(guildNameNode);
				if (guildTpNode != null) {
					guildRootGroupData.remove(guildTpNode);
				}
				LuckPermsIntegration.setGuildBanner(sender, guildRootGroup, plugin, guildBanner, true);
				LuckPermsIntegration.GM.saveGroup(guildRootGroup).join();

				Bukkit.getScheduler().runTask(plugin, ()
					-> sender.sendMessage(Component.text("Deleting old guild group ID...", NamedTextColor.GOLD)));
				LuckPermsIntegration.GM.deleteGroup(oldGuildGroup);

				LuckPermsIntegration.pushUpdate();

				String finalGuildTag = guildTag;
				TextColor finalGuildColor = guildColor;
				Bukkit.getScheduler().runTask(plugin, () -> {
					sender.sendMessage(Component.text("Configuring guild chat, feel free to override once set.",
						NamedTextColor.GOLD));
					String channelPerm = "group." + guildMemberGroupId;
					Channel guildChannelTest = MonumentaNetworkChatIntegration.getChannel(finalGuildTag);
					ChannelGlobal guildChannel = null;
					if (guildChannelTest == null) {
						guildChannel = MonumentaNetworkChatIntegration.createGuildChannel(sender, finalGuildTag, guildMemberGroupId);
						if (guildChannel == null) {
							sender.sendMessage(Component.text("Unable to create guild chat for "
									+ finalGuildTag,
								NamedTextColor.RED));
						}
					} else if (guildChannelTest instanceof ChannelGlobal) {
						guildChannel = (ChannelGlobal) guildChannelTest;
						guildChannel.setChannelPermission(channelPerm);
					} else {
						sender.sendMessage(Component.text("There is already a chat channel named "
								+ finalGuildTag + ", but it is not a guild channel. This must be fixed manually.",
							NamedTextColor.RED));
					}
					if (guildChannel != null) {
						try {
							if (guildChannel.color() == null) {
								// Set color if not previously specified
								guildChannel.color(sender, finalGuildColor);
							}
							MonumentaNetworkChatIntegration.saveChannel(guildChannel);
						} catch (WrapperCommandSyntaxException e) {
							sender.sendMessage(Component.text("Unable to set chat color for "
								+ finalGuildTag, NamedTextColor.RED));
						}

						for (Player player : Bukkit.getOnlinePlayers()) {
							if (player.hasPermission(channelPerm)) {
								MonumentaNetworkChatIntegration.setPlayerDefaultGuildChat(player, guildChannel);
							}
						}
					}

					sender.sendMessage(Component.text("Done", NamedTextColor.GOLD));
				});
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(plugin, () -> {
					sender.sendMessage(Component.text("An exception occurred upgrading the guild:",
						NamedTextColor.RED));
					MessagingUtils.sendStackTrace(sender, ex);
				});
			}
		});
	}
}

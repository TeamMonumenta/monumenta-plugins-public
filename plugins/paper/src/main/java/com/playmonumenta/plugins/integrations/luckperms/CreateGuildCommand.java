package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.networkchat.channel.ChannelGlobal;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.RBoardAPI;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.FireworkMeta;
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
import static com.playmonumenta.plugins.utils.MessagingUtils.ESCAPED_TEXT_COLOR_SUGGESTIONS;

public class CreateGuildCommand {
	private static final String[] SUGGESTIONS = {"@a[x=-770,y=106,z=-128,dx=7,dy=4,dz=13,gamemode=!spectator]"};

	@SuppressWarnings({"DataFlowIssue"})
	public static void register(Plugin plugin) {
		// guild mod create <guild name> <guild color> <guild tag> <founders>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.guild.mod.create");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("mod"));
		arguments.add(new LiteralArgument("create"));
		arguments.add(new TextArgument("guild name")
			.replaceSuggestions(GuildArguments.NAME_SUGGESTIONS));
		arguments.add(new TextArgument("guild color")
			.replaceSuggestions(ESCAPED_TEXT_COLOR_SUGGESTIONS));
		arguments.add(new TextArgument("guild tag")
			.replaceSuggestions(GuildArguments.TAG_SUGGESTIONS));
		arguments.add(new EntitySelectorArgument.ManyPlayers("founders")
			.replaceSuggestions(ArgumentSuggestions.strings(info -> SUGGESTIONS)));

		new CommandAPICommand("guild")
			.withArguments(arguments)
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, perms);
				if (ServerProperties.getShardName().contains("build")) {
					throw CommandAPI.failWithString("This command cannot be run on the build shard.");
				}

				String guildName = args.getUnchecked("guild name");
				String guildColor = args.getUnchecked("guild color");
				String guildTag = args.getUnchecked("guild tag");
				Collection<Player> guildFounders = args.getUnchecked("founders");
				run(plugin, sender, guildName, guildColor, guildTag, guildFounders);
			})
			.register();
	}

	private static void run(Plugin plugin,
	                        CommandSender sender,
	                        String guildName,
	                        String guildColorString,
	                        String guildTag,
	                        Collection<Player> founders)
		throws WrapperCommandSyntaxException {

		if (guildTag.length() > LuckPermsIntegration.MAX_TAG_LENGTH) {
			throw CommandAPI.failWithString("Guild tag cannot exceed " + LuckPermsIntegration.MAX_TAG_LENGTH + " characters");
		}

		if (MonumentaNetworkChatIntegration.getChannel(guildTag) != null) {
			throw CommandAPI.failWithString("The chat channel for this guild already exists: " + guildTag);
		}

		@Nullable TextColor guildColor = MessagingUtils.colorFromString(guildColorString);
		if (guildColor == null) {
			throw CommandAPI.failWithString("Invalid guild color: " + guildColorString);
		}
		String guildTagPrefix = guildColor.asHexString() + "[" + guildTag + "]";

		if (!(sender instanceof Player senderPlayer)) {
			throw CommandAPI.failWithString("Guilds must be founded by a player");
		}

		ItemStack guildBannerTemplate = senderPlayer.getInventory().getItemInMainHand();
		if (!ItemUtils.isBanner(guildBannerTemplate)) {
			throw CommandAPI.failWithString("Please hold the guild banner when founding a guild");
		}
		ItemMeta templateMeta = guildBannerTemplate.getItemMeta();

		final ItemStack guildBanner = new ItemStack(guildBannerTemplate.getType());
		ItemMeta meta = guildBanner.getItemMeta();
		if (templateMeta instanceof BannerMeta templateBannerMeta
			&& meta instanceof BannerMeta bannerMeta) {
			bannerMeta.setPatterns(templateBannerMeta.getPatterns());
		}
		guildBanner.setItemMeta(meta);

		boolean hasEnoughLevels = true;
		boolean inGuildAlready = false;
		for (Player founder : founders) {
			if (LuckPermsIntegration.getGuild(founder) != null) {
				sender.sendMessage(Component.text("Player " + founder.getName() + " is already in a guild!", NamedTextColor.DARK_RED, TextDecoration.ITALIC));
				inGuildAlready = true;
			}

			int level = ScoreboardUtils.getScoreboardValue(founder, AbilityUtils.TOTAL_LEVEL).orElse(0);
			if (level < 5) {
				sender.sendMessage(Component.text("The minimal level for " + founder.getName() + " is not reached (" + level + "/5)", NamedTextColor.DARK_RED, TextDecoration.ITALIC));
				hasEnoughLevels = false;
			}
		}

		// Displays ALL founders in a guild / without enough levels
		if (inGuildAlready) {
			throw CommandAPI.failWithString("At least one founder is already in a guild");
		}
		if (!hasEnoughLevels) {
			throw CommandAPI.failWithString("Individual founder level requirements not met");
		}

		// Guild name sanitization for command usage
		String guildRootGroupId = LuckPermsIntegration.getGuildId(guildTag);
		String guildGuestGroupId = GuildAccessLevel.GUEST.groupNameFromRoot(guildRootGroupId);
		String guildMemberGroupId = GuildAccessLevel.MEMBER.groupNameFromRoot(guildRootGroupId);
		String guildManagerGroupId = GuildAccessLevel.MANAGER.groupNameFromRoot(guildRootGroupId);
		String guildFounderGroupId = GuildAccessLevel.FOUNDER.groupNameFromRoot(guildRootGroupId);

		String guildGuestInviteGroupId = GuildInviteLevel.GUEST_INVITE.groupNameFromRoot(guildRootGroupId);
		String guildMemberInviteGroupId = GuildInviteLevel.MEMBER_INVITE.groupNameFromRoot(guildRootGroupId);

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			for (String groupId : List.of(guildRootGroupId,
				guildGuestGroupId,
				guildMemberGroupId,
				guildManagerGroupId,
				guildFounderGroupId,
				guildGuestInviteGroupId,
				guildMemberInviteGroupId)) {
				try {
					if (LuckPermsIntegration.GM.loadGroup(groupId).join().isPresent()) {
						Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(Component.text(
							"The luckperms group '" + groupId + "' already exists!", NamedTextColor.RED)));
						return;
					}
				} catch (Throwable throwable) {
					Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(Component.text(
						"Failed to check luckperms group '" + groupId + "'!", NamedTextColor.RED)));
					return;
				}
			}

			// Get guild number
			long guildPlot;
			try {
				guildPlot = RBoardAPI.add("$Last", "GuildPlot", 1).join();
			} catch (Throwable throwable) {
				Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(Component.text(
					"Failed to get new guild number for '" + guildName + "'!", NamedTextColor.RED)));
				return;
			}

			// Create root node (used for frozen state and accessing guild info; not used for player's guild)
			Group guildRootGroup = LuckPermsIntegration.GM.createAndLoadGroup(guildRootGroupId).join();
			NodeMap guildRootGroupData = guildRootGroup.data();
			guildRootGroupData.add(MetaNode.builder(GUILD_ROOT_ID_MK, guildRootGroupId).build());
			guildRootGroupData.add(MetaNode.builder(GUILD_ROOT_PLOT_MK, String.valueOf(guildPlot)).build());
			guildRootGroupData.add(MetaNode.builder(GUILD_ROOT_COLOR_MK, guildColor.asHexString()).build());
			guildRootGroupData.add(MetaNode.builder(GUILD_ROOT_PLAIN_TAG_MK, guildTag).build());
			guildRootGroupData.add(MetaNode.builder(GUILD_ROOT_TAG_MK, guildTagPrefix).build());
			guildRootGroupData.add(MetaNode.builder(GUILD_ROOT_NAME_MK, guildName).build());
			LuckPermsIntegration.GM.saveGroup(guildRootGroup).join();
			LuckPermsIntegration.setGuildBanner(sender, guildRootGroup, plugin, guildBanner, true);

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
			guildGuestGroupData.add(InheritanceNode.builder(guildRootGroup).build());

			Group guildMemberGroup = LuckPermsIntegration.GM.createAndLoadGroup(guildMemberGroupId).join();
			NodeMap guildMemberGroupData = guildMemberGroup.data();
			guildMemberGroupData.add(InheritanceNode.builder(guildGuestGroup).build());
			guildMemberGroupData.add(MetaNode.builder(GUILD_MEMBER_ROOT_ID_MK, guildRootGroupId).build());
			guildMemberGroupData.add(MetaNode.builder(GUILD_MEMBER_ROOT_PLOT_MK, String.valueOf(guildPlot)).build());
			guildMemberGroupData.add(MetaNode.builder(GUILD_MEMBER_ROOT_COLOR_MK, guildColor.asHexString()).build());
			guildMemberGroupData.add(MetaNode.builder(GUILD_MEMBER_ROOT_PLAIN_TAG_MK, guildTag).build());
			guildMemberGroupData.add(PrefixNode.builder(guildTagPrefix, 1).build());
			guildMemberGroupData.add(MetaNode.builder(GUILD_MEMBER_HOVER_PREFIX_MK, guildName).build());
			guildMemberGroupData.add(MetaNode.builder(GUILD_MEMBER_GUILD_NAME_MK, guildName).build());

			Group guildManagerGroup = LuckPermsIntegration.GM.createAndLoadGroup(guildManagerGroupId).join();
			NodeMap guildManagerGroupData = guildManagerGroup.data();
			guildManagerGroupData.add(InheritanceNode.builder(guildMemberGroup).build());

			Group guildFounderGroup = LuckPermsIntegration.GM.createAndLoadGroup(guildFounderGroupId).join();
			NodeMap guildFounderGroupData = guildFounderGroup.data();
			guildFounderGroupData.add(InheritanceNode.builder(guildManagerGroup).build());

			for (GuildPermission guildPermission : GuildPermission.values()) {
				guildPermission.setExplicitPermission(guildRootGroup, guildMemberGroup, true);
			}

			LuckPermsIntegration.GM.saveGroup(guildGuestGroup).join();
			LuckPermsIntegration.GM.saveGroup(guildMemberGroup).join();
			LuckPermsIntegration.GM.saveGroup(guildManagerGroup).join();
			LuckPermsIntegration.GM.saveGroup(guildFounderGroup).join();

			for (Player founder : founders) {
				User user = LuckPermsIntegration.UM.getUser(founder.getUniqueId());
				if (user == null) {
					Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(Component.text(
						"No such luckperms user " + founder.getName(), NamedTextColor.RED)));
					continue;
				}
				user.data().add(InheritanceNode.builder(guildFounderGroup).build());
				LuckPermsIntegration.UM.saveUser(user).join();
			}

			LuckPermsIntegration.pushUpdate();

			Bukkit.getScheduler().runTask(plugin, () -> {
				// Create guild chat channel
				String chatPerm = GuildPermission.CHAT.guildPermissionString(guildRootGroup);
				ChannelGlobal guildChannel = null;
				if (chatPerm == null) {
					sender.sendMessage(Component.text("Could not get chat permission for "
							+ guildTag + ".",
						NamedTextColor.RED));
				} else {
					guildChannel = MonumentaNetworkChatIntegration.createGuildChannel(sender, guildTag, chatPerm);
					if (guildChannel == null) {
						sender.sendMessage(Component.text("The guild channel "
								+ guildTag
								+ " could not be created. The rest of the guild should be set up correctly.",
							NamedTextColor.RED));
					} else {
						try {
							guildChannel.color(sender, guildColor);
							MonumentaNetworkChatIntegration.saveChannel(guildChannel);
						} catch (WrapperCommandSyntaxException e) {
							sender.sendMessage(Component.text("Failed to set guild chat channel's color.",
								NamedTextColor.RED));
						}
					}
				}

				// Add tags, display messages and effects
				for (Player founder : founders) {
					founder.sendMessage(Component.text("Congratulations! You have founded a new guild!", NamedTextColor.GOLD));
					founder.playSound(founder.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 1f, 1.5f);

					// Update chat info
					if (guildChannel != null) {
						MonumentaNetworkChatIntegration.setPlayerDefaultGuildChat(founder, guildChannel);
					}
					MonumentaNetworkChatIntegration.refreshPlayer(founder);

					// fireworks!
					Location location = founder.getLocation();
					location.getWorld().spawn(location, Firework.class, (Firework firework) -> {
						FireworkMeta fireworkMeta = firework.getFireworkMeta();
						fireworkMeta.addEffect(FireworkEffect.builder()
							.with(FireworkEffect.Type.BALL_LARGE)
							.withColor(Color.fromRGB(guildColor.red(), guildColor.green(), guildColor.blue()))
							.build());
						firework.setFireworkMeta(fireworkMeta);
						firework.setTicksToDetonate(0);
					});
				}

				Component announcementMessage = Component.text(
					"A new guild has just been founded. Say hello to ",
						NamedTextColor.WHITE,
						TextDecoration.BOLD)
					.append(Component.text(guildName, guildColor)
						.hoverEvent(guildBanner))
					.append(Component.text("!!"));

				MonumentaNetworkRelayIntegration.broadcastCommand("tellraw @a[all_worlds=true] "
					+ MessagingUtils.toGson(announcementMessage));
			});
		});
	}
}

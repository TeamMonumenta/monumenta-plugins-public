package com.playmonumenta.plugins.integrations.luckperms;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.playmonumenta.networkchat.channel.Channel;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import com.playmonumenta.plugins.integrations.luckperms.guildgui.GuildGui;
import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.integrations.luckperms.listeners.InviteNotification;
import com.playmonumenta.plugins.integrations.luckperms.listeners.LPArguments;
import com.playmonumenta.plugins.integrations.luckperms.listeners.Lockdown;
import com.playmonumenta.plugins.integrations.luckperms.listeners.RefreshChat;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.messaging.MessagingService;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.matcher.NodeMatcher;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.query.Flag;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.profile.PlayerTextures;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class LuckPermsIntegration implements Listener {
	public static final int MAX_TAG_LENGTH = 10;
	public static final int META_VALUE_SECTION_SIZE = 64;
	public static final char META_KEY_NODE_SEPARATOR = ':';
	public static final String META_KEY_LENGTH_INDICATOR = "length";
	public static final String GUILD_MK = "guild";
	public static final String GUILD_ROOT_MK = GUILD_MK + META_KEY_NODE_SEPARATOR + "root";
	public static final String GUILD_ROOT_ID_MK = GUILD_ROOT_MK + META_KEY_NODE_SEPARATOR + "id";
	public static final String GUILD_ROOT_LOCKDOWN_MK = GUILD_ROOT_MK + META_KEY_NODE_SEPARATOR + "lockdown";
	public static final String GUILD_ROOT_PLOT_MK = GUILD_ROOT_MK + META_KEY_NODE_SEPARATOR + "plot";
	public static final String GUILD_ROOT_COLOR_MK = GUILD_ROOT_MK + META_KEY_NODE_SEPARATOR + "color";
	public static final String GUILD_ROOT_PLAIN_TAG_MK = GUILD_ROOT_MK + META_KEY_NODE_SEPARATOR + "plain_tag";
	public static final String GUILD_ROOT_TAG_MK = GUILD_ROOT_MK + META_KEY_NODE_SEPARATOR + "tag";
	public static final String GUILD_ROOT_NAME_MK = GUILD_ROOT_MK + META_KEY_NODE_SEPARATOR + "name";
	public static final String GUILD_ROOT_BANNER_MK = GUILD_ROOT_MK + META_KEY_NODE_SEPARATOR + "banner";
	public static final String GUILD_ROOT_BANNER_LENGTH_MK = GUILD_ROOT_BANNER_MK + META_KEY_NODE_SEPARATOR + META_KEY_LENGTH_INDICATOR;
	public static final String GUILD_MEMBER_MK = GUILD_MK + META_KEY_NODE_SEPARATOR + "member";
	public static final String GUILD_MEMBER_ROOT_MK = GUILD_MEMBER_MK + META_KEY_NODE_SEPARATOR + "root";
	public static final String GUILD_MEMBER_ROOT_ID_MK = GUILD_MEMBER_ROOT_MK + META_KEY_NODE_SEPARATOR + "id";
	public static final String GUILD_MEMBER_ROOT_PLOT_MK = GUILD_MEMBER_ROOT_MK + META_KEY_NODE_SEPARATOR + "plot";
	public static final String GUILD_MEMBER_ROOT_COLOR_MK = GUILD_MEMBER_ROOT_MK + META_KEY_NODE_SEPARATOR + "color";
	public static final String GUILD_MEMBER_ROOT_PLAIN_TAG_MK = GUILD_MEMBER_ROOT_MK + META_KEY_NODE_SEPARATOR + "plain_tag";
	public static final String GUILD_MEMBER_HOVER_PREFIX_MK = "hoverprefix";
	public static final String GUILD_MEMBER_GUILD_NAME_MK = "guildname";
	public static final String GUILD_TP_MK = "guildtp";
	public static final PlayerProfile ERROR_QUESTION_MARK_PLAYER_PROFILE;

	public static final Set<String> CHAT_META_KEYS = Set.of(
		GUILD_ROOT_TAG_MK,
		GUILD_MEMBER_HOVER_PREFIX_MK,
		GUILD_MEMBER_GUILD_NAME_MK
	);

	static {
		ERROR_QUESTION_MARK_PLAYER_PROFILE = Bukkit.createProfile(
			UUID.fromString("9389d101-1a9d-4050-b1ac-acee374266e1"), "NackNickGus");

		URL skinUrl;
		try {
			skinUrl = new URL("http://textures.minecraft.net/texture/4091640da9e8fe0bf259919ecce7ddef87aaf8ba6eabfbaacf2df1d2a24d80d9");
		} catch (MalformedURLException e) {
			skinUrl = null;
		}

		if (skinUrl != null) {
			PlayerTextures textures = ERROR_QUESTION_MARK_PLAYER_PROFILE.getTextures();
			textures.setSkin(skinUrl, PlayerTextures.SkinModel.CLASSIC);
			ERROR_QUESTION_MARK_PLAYER_PROFILE.setTextures(textures);
		}
	}

	protected static @MonotonicNonNull LuckPerms LP = null;
	protected static @MonotonicNonNull UserManager UM = null;
	protected static @MonotonicNonNull GroupManager GM = null;

	public static class GroupChildrenAndMembers {
		String mName;
		Map<String, GroupChildrenAndMembers> mChildGroups = new TreeMap<>();
		Set<UUID> mMembers = new HashSet<>();

		public GroupChildrenAndMembers(String name) {
			mName = name;
		}
	}

	public static void enable(Plugin plugin) {
		plugin.getLogger().info("Enabling LuckPerms integration");
		RegisteredServiceProvider<LuckPerms> luckPermsProvider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
		if (luckPermsProvider == null) {
			plugin.getLogger().severe("Failed to load LuckPerms registration");
			return;
		}
		LP = luckPermsProvider.getProvider();
		UM = LP.getUserManager();
		GM = LP.getGroupManager();

		BulkGuildBanners.register(plugin);
		CreateGuildCommand.register(plugin);
		GetGuildBanner.register(plugin);
		GuildCommand.register(plugin);
		GuildGui.register(plugin);
		ListGuilds.register(plugin);
		LpGroupDeleteCommand.register(plugin);
		LpGroupListCommand.register(plugin);
		LpGroupRenameCommand.register(plugin);
		OffDutyCommand.register();
		SetGuildTeleport.register(plugin);
		TeleportGuild.register();
		TeleportGuildGui.register(plugin);
		TestGuild.register();
		TransferLPPermissions.register(plugin);
		UpgradeGuild.register(plugin);

		EventBus eventBus = LP.getEventBus();
		GuildArguments.registerLuckPermsEvents(plugin, eventBus);
		InviteNotification.registerLuckPermsEvents(plugin, eventBus);
		Lockdown.registerLuckPermsEvents(plugin, eventBus);
		LPArguments.registerLuckPermsEvents(plugin, eventBus);
		RefreshChat.registerLuckPermsEvents(plugin, eventBus);
	}

	public static Set<User> getLoadedUsers() {
		return UM.getLoadedUsers();
	}

	public static Set<Group> getLoadedGroups() {
		return GM.getLoadedGroups();
	}

	public static Set<Group> getLoadedGuilds() {
		Set<Group> guildRoots = new HashSet<>();

		nextGroup:
		for (Group group : GM.getLoadedGroups()) {
			for (MetaNode node : group.getNodes(NodeType.META)) {
				if (node.getMetaKey().equals(GUILD_ROOT_ID_MK)) {
					guildRoots.add(group);
					continue nextGroup;
				}
			}
		}

		return guildRoots;
	}

	public static Set<Group> getLoadedGuildLevelGroup(GuildAccessLevel accessLevel) {
		Set<Group> result = new HashSet<>();

		// Groups cannot be loaded without their inherited groups also being loaded
		for (Group guildRoot : getLoadedGuilds()) {
			// Access level may not be loaded; depends on if an online player has at least that access level
			Group guildAccessGroup = accessLevel.getLoadedGroupFromRoot(guildRoot);
			if (guildAccessGroup != null) {
				result.add(guildAccessGroup);
			}
		}

		return result;
	}

	public static CompletableFuture<List<Group>> getGuilds() {
		return getGuilds(false, false);
	}

	public static CompletableFuture<List<Group>> getGuilds(boolean includeLegacy, boolean onlyLegacy) {
		CompletableFuture<List<Group>> result = new CompletableFuture<>();
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				String targetMetaKey = includeLegacy ? GUILD_MEMBER_GUILD_NAME_MK : GUILD_MEMBER_ROOT_ID_MK;
				Map<String, Collection<MetaNode>> guildNodeMatches
					= GM.searchAll(NodeMatcher.metaKey(targetMetaKey)).join();

				List<Group> groups = new ArrayList<>();
				for (Map.Entry<String, Collection<MetaNode>> guildMatch : guildNodeMatches.entrySet()) {
					String guildId = guildMatch.getKey();
					Optional<Group> optGroup = GM.loadGroup(guildId).join();
					if (optGroup.isPresent()) {
						Group group = optGroup.get();
						if (onlyLegacy && isModern(group)) {
							continue;
						}
						groups.add(group);
					}
				}
				result.complete(groups);
			} catch (Exception ex) {
				result.completeExceptionally(ex);
			}
		});
		return result;
	}

	public static CompletableFuture<List<Group>> getGuildRoots() {
		CompletableFuture<List<Group>> result = new CompletableFuture<>();
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				Map<String, Collection<MetaNode>> guildNodeMatches
					= GM.searchAll(NodeMatcher.metaKey(GUILD_ROOT_ID_MK)).join();

				List<Group> groups = new ArrayList<>();
				for (Map.Entry<String, Collection<MetaNode>> guildMatch : guildNodeMatches.entrySet()) {
					String guildId = guildMatch.getKey();
					Optional<Group> optGroup = GM.loadGroup(guildId).join();
					optGroup.ifPresent(groups::add);
				}
				result.complete(groups);
			} catch (Exception ex) {
				result.completeExceptionally(ex);
			}
		});
		return result;
	}

	public static User getUser(Player player) {
		return UM.getUser(player.getUniqueId());
	}

	public static void setPermission(Player player, String permission, boolean value) {
		UM.modifyUser(player.getUniqueId(), user -> {
			// Add the permission
			user.data().add(Node.builder(permission).value(value).build());
		});
	}

	/**
	 * Gets the main guild of a player
	 *
	 * @param player The player whose guild you wish to check
	 * @return The player's guild, returning the matching member/manager/founder group, or else null
	 */
	public static @Nullable Group getGuild(Player player) {
		return getGuild(UM.getUser(player.getUniqueId()));
	}

	/**
	 * Gets the main guild of a player
	 *
	 * @param user The player whose guild you wish to check
	 * @return The player's guild, returning the matching member/manager/founder group, or else null
	 */
	public static @Nullable Group getGuild(@Nullable User user) {
		if (user == null) {
			return null;
		}
		Set<String> parentGroupNames = new HashSet<>();
		for (InheritanceNode parentNode : user.getNodes(NodeType.INHERITANCE)) {
			parentGroupNames.add(parentNode.getGroupName());
		}
		for (Group group : user.getInheritedGroups(QueryOptions.nonContextual())) {
			if (!parentGroupNames.contains(group.getName())) {
				continue;
			}
			for (MetaNode node : group.resolveInheritedNodes(NodeType.META, QueryOptions.nonContextual())) {
				if (node.getMetaKey().equals(GUILD_MEMBER_GUILD_NAME_MK)) {
					return group;
				}
			}
		}

		return null;
	}

	@Contract("null -> false")
	public static boolean isModern(@Nullable Group guild) {
		return getGuildRoot(guild) != null;
	}

	public static Set<Group> getRelevantGuilds(Player player, boolean includeMembership, boolean includeInvited) {
		User user = UM.getUser(player.getUniqueId());
		if (user == null) {
			return Set.of();
		}
		return getRelevantGuilds(user, includeMembership, includeInvited);
	}

	public static Set<Group> getRelevantGuilds(User user, boolean includeMembership, boolean includeInvited) {
		Set<String> parentGroupNames = new HashSet<>();
		for (InheritanceNode parentNode : user.getNodes(NodeType.INHERITANCE)) {
			parentGroupNames.add(parentNode.getGroupName());
		}
		Set<Group> guilds = new HashSet<>();
		for (Group group : user.getInheritedGroups(QueryOptions.nonContextual())) {
			if (!parentGroupNames.contains(group.getName())) {
				continue;
			}
			for (MetaNode node : group.resolveInheritedNodes(NodeType.META, QueryOptions.nonContextual())) {
				if (!node.getMetaKey().equals(GUILD_ROOT_ID_MK)) {
					continue;
				}

				GuildInviteLevel inviteLevel = GuildInviteLevel.byGroup(group);
				if (!GuildInviteLevel.NONE.equals(inviteLevel)) {
					if (includeInvited) {
						guilds.add(group);
					}
					break;
				}

				GuildAccessLevel accessLevel = GuildAccessLevel.byGroup(group);
				if (!GuildAccessLevel.NONE.equals(accessLevel)) {
					if (includeMembership) {
						guilds.add(group);
					}
					break;
				}
			}
		}
		return guilds;
	}

	public static GuildInviteLevel getInviteLevel(Group guildRoot, User user) {
		QueryOptions options = QueryOptions.builder(QueryMode.CONTEXTUAL).flag(Flag.RESOLVE_INHERITANCE, true).build();

		GuildInviteLevel level = GuildInviteLevel.NONE;
		for (Group group : user.getInheritedGroups(options)) {
			if (!group.getName().startsWith(guildRoot.getName()) || group.getName().equals(guildRoot.getName())) {
				continue;
			}
			GuildInviteLevel groupInviteLevel = GuildInviteLevel.byGroup(group);

			if (!group.getName().equals(groupInviteLevel.groupNameFromRoot(guildRoot))) {
				// False match; there's additional text between the prefix and suffix
				continue;
			}

			if (level.ordinal() > groupInviteLevel.ordinal()) {
				level = groupInviteLevel;
			}
		}

		return level;
	}

	public static GuildAccessLevel getAccessLevel(Group guildRoot, User user) {
		QueryOptions options = QueryOptions.builder(QueryMode.CONTEXTUAL).flag(Flag.RESOLVE_INHERITANCE, true).build();

		GuildAccessLevel level = GuildAccessLevel.NONE;
		for (Group group : user.getInheritedGroups(options)) {
			if (!group.getName().startsWith(guildRoot.getName()) || group.getName().equals(guildRoot.getName())) {
				continue;
			}
			GuildAccessLevel groupAccessLevel = GuildAccessLevel.byGroup(group);

			if (!group.getName().equals(groupAccessLevel.groupNameFromRoot(guildRoot))) {
				// False match; there's additional text between the prefix and suffix
				continue;
			}

			if (level.ordinal() > groupAccessLevel.ordinal()) {
				level = groupAccessLevel;
			}
		}

		return level;
	}

	@Contract("null -> null")
	public static @Nullable Group getGuildRoot(@Nullable Group guild) {
		if (guild == null) {
			return null;
		}

		// In case the provided group is the root node
		for (MetaNode node : guild.getNodes(NodeType.META)) {
			if (node.getMetaKey().equals(GUILD_ROOT_ID_MK)) {
				return guild;
			}
		}

		// Check parent groups
		for (Group group : guild.getInheritedGroups(QueryOptions.nonContextual())) {
			for (MetaNode node : group.getNodes(NodeType.META)) {
				if (node.getMetaKey().equals(GUILD_ROOT_ID_MK)) {
					return group;
				}
			}
		}

		// Not found
		return null;
	}

	public static @Nullable TextColor getGuildColor(@Nullable Group group) {
		if (group == null) {
			return null;
		}

		for (MetaNode node : group.resolveInheritedNodes(NodeType.META, QueryOptions.nonContextual())) {
			if (node.getMetaKey().equals(GUILD_ROOT_COLOR_MK)) {
				return TextColor.fromHexString(node.getMetaValue());
			}
		}

		return null;
	}

	public static @Nullable String getGuildPlainTag(Group group) {
		Group guildRoot = getGuildRoot(group);
		if (guildRoot == null) {
			return null;
		}
		for (MetaNode node : guildRoot.resolveInheritedNodes(NodeType.META, QueryOptions.nonContextual())) {
			if (node.getMetaKey().equals(GUILD_ROOT_PLAIN_TAG_MK)) {
				return node.getMetaValue();
			}
		}

		return null;
	}

	public static @Nullable String getRawGuildName(Group group) {
		Group guildRoot = getGuildRoot(group);
		if (guildRoot == null) {
			// Legacy guild
			for (MetaNode node : group.resolveInheritedNodes(NodeType.META, QueryOptions.nonContextual())) {
				if (node.getMetaKey().equals(GUILD_MEMBER_GUILD_NAME_MK)) {
					return node.getMetaValue();
				}
			}
			return null;
		}
		for (MetaNode node : guildRoot.resolveInheritedNodes(NodeType.META, QueryOptions.nonContextual())) {
			if (node.getMetaKey().equals(GUILD_ROOT_NAME_MK)) {
				return node.getMetaValue();
			}
		}
		return null;
	}

	public static String getNonNullGuildName(Group group) {
		Group guildRoot = getGuildRoot(group);
		if (guildRoot == null) {
			// Legacy guild
			for (MetaNode node : group.resolveInheritedNodes(NodeType.META, QueryOptions.nonContextual())) {
				if (node.getMetaKey().equals(GUILD_MEMBER_GUILD_NAME_MK)) {
					return node.getMetaValue() + " <Guild is legacy and needs updating>";
				}
			}
			return group.getName() + " <Guild is legacy and needs updating>";
		}
		for (MetaNode node : guildRoot.resolveInheritedNodes(NodeType.META, QueryOptions.nonContextual())) {
			if (node.getMetaKey().equals(GUILD_ROOT_NAME_MK)) {
				return node.getMetaValue();
			}
		}
		return group.getName() + " <Guild name is not set>";
	}

	public static Component getGuildFullComponent(Group guild) {
		TextColor guildColor = null;
		String guildPlainTag = "";
		Component guildName = Component.empty();
		for (MetaNode node : guild.resolveInheritedNodes(NodeType.META, QueryOptions.nonContextual())) {
			switch (node.getMetaKey()) {
				case GUILD_ROOT_COLOR_MK -> guildColor = TextColor.fromHexString(node.getMetaValue());
				case GUILD_ROOT_PLAIN_TAG_MK -> guildPlainTag = "[" + node.getMetaValue() + "]";
				case GUILD_ROOT_NAME_MK -> guildName = Component.text(node.getMetaValue());
				default -> {
				}
			}
		}

		Component guildTag;
		if (guildColor == null) {
			// Legacy guild - preview upgrade without performing upgrade
			for (Node node : guild.getNodes()) {
				if (node instanceof PrefixNode thePrefixNode) {
					String prefixTag = thePrefixNode.getMetaValue();
					if (prefixTag.length() < 7 || !prefixTag.startsWith("#")) {
						if (!prefixTag.isEmpty()) {
							guildColor = MessagingUtils.colorByHexit(prefixTag.charAt(0));
							if (guildColor == null) {
								guildPlainTag = prefixTag.substring(1);
							} else {
								guildPlainTag = prefixTag;
							}
						}
					} else {
						guildPlainTag = prefixTag.substring(7);
						String guildColorString = prefixTag.substring(0, 7);
						guildColor = TextColor.fromHexString(guildColorString);
					}
				} else if (node instanceof MetaNode metaNode) {
					if (metaNode.getMetaKey().equals(GUILD_MEMBER_GUILD_NAME_MK)) {
						guildName = Component.text(metaNode.getMetaValue());
					}
				}
			}
		}

		if (guildColor == null) {
			guildTag = Component.text(guildPlainTag);
		} else {
			guildTag = Component.text(guildPlainTag, guildColor);
		}

		return Component.empty()
			.append(guildTag)
			.append(Component.space())
			.append(guildName);
	}

	public static @Nullable String getUnlockedGuildName(@Nullable Group group) {
		if (group == null) {
			return null;
		}
		Group guildRoot = getGuildRoot(group);
		if (guildRoot == null) {
			return null;
		}
		String guildName = null;
		for (MetaNode node : guildRoot.resolveInheritedNodes(NodeType.META, QueryOptions.nonContextual())) {
			if (node.getMetaKey().equals(GUILD_ROOT_LOCKDOWN_MK)) {
				return null;
			}
			if (node.getMetaKey().equals(GUILD_ROOT_NAME_MK)) {
				guildName = node.getMetaValue();
			}
		}

		return guildName;
	}

	public static void setGuildTp(CommandSender sender, Group group, Plugin plugin, Location loc) {
		Group root = getGuildRoot(group);
		if (root == null) {
			sender.sendMessage(Component.text("Specified group is not a modern guild", NamedTextColor.RED));
			return;
		}

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				Optional<Group> optGroup = GuildAccessLevel.GUEST.loadGroupFromRoot(root).join();
				if (optGroup.isEmpty()) {
					throw new Exception("No guest group found for that guild.");
				}
				Group guestGroup = optGroup.get();

				// Remove all the other guildtp meta nodes
				for (MetaNode node : guestGroup.getNodes(NodeType.META)) {
					if (node.getMetaKey().equals(GUILD_TP_MK)) {
						guestGroup.data().remove(node);
					}
				}

				guestGroup.data().add(MetaNode.builder(GUILD_TP_MK, LocationUtils.locationToString(loc)).build());

				GM.saveGroup(guestGroup).join();

				Bukkit.getScheduler().runTask(plugin,
					() -> sender.sendMessage(Component.text("Set guild TP", NamedTextColor.GOLD)));
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(plugin, () -> {
					sender.sendMessage(Component.text("Could not set guild TP:", NamedTextColor.RED));
					MessagingUtils.sendStackTrace(sender, ex);
				});
			}
		});
	}

	public static CompletableFuture<Optional<Location>> getGuildTp(World world, Group group) {
		CompletableFuture<Optional<Location>> result = new CompletableFuture<>();
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				Group root = getGuildRoot(group);
				if (root == null) {
					result.complete(Optional.empty());
					return;
				}

				Optional<Group> optGuestGroup = GuildAccessLevel.GUEST.loadGroupFromRoot(root).join();
				if (optGuestGroup.isEmpty()) {
					result.complete(Optional.empty());
					return;
				}
				Group guestGroup = optGuestGroup.get();

				Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
					try {
						for (MetaNode node : guestGroup.resolveInheritedNodes(NodeType.META, QueryOptions.nonContextual())) {
							if (node.getMetaKey().equals(GUILD_TP_MK)) {
								result.complete(Optional.of(LocationUtils.locationFromString(world, node.getMetaValue())));
								return;
							}
						}

						result.complete(Optional.empty());
					} catch (Exception ex) {
						result.complete(Optional.empty());
					}
				});
			} catch (Exception ex) {
				result.complete(Optional.empty());
			}
		});
		return result;
	}

	public static @Nullable Long getGuildPlotId(Group group) {
		try {
			Group root = getGuildRoot(group);
			if (root == null) {
				return null;
			}

			for (MetaNode node : root.getNodes(NodeType.META)) {
				if (node.getMetaKey().equals(GUILD_ROOT_PLOT_MK)) {
					return Long.parseLong(node.getMetaValue());
				}
			}
		} catch (Exception ex) {
			MMLog.warning("An error occurred getting the guild plot ID:");
			MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
			return null;
		}

		return null;
	}

	public static CompletableFuture<User> loadUser(UUID playerId) {
		return UM.loadUser(playerId);
	}

	// Get a group (sync) if it is already loaded
	public static @Nullable Group getGroup(@Nullable String groupName) {
		if (groupName == null) {
			return null;
		}

		return GM.getGroup(groupName);
	}

	// Load a group (async)
	public static CompletableFuture<Optional<Group>> loadGroup(@Nullable String groupName) {
		if (groupName == null) {
			return CompletableFuture.completedFuture(Optional.empty());
		}

		return GM.loadGroup(groupName);
	}

	public static CompletableFuture<Void> loadAllGroups() {
		return GM.loadAllGroups();
	}

	public static CompletableFuture<Set<UUID>> getGroupMembers(Group group) {
		return getGroupMembers(InheritanceNode.builder(group).build());
	}

	public static CompletableFuture<Set<UUID>> getGroupMembers(InheritanceNode groupInheritanceNode) {
		CompletableFuture<Set<UUID>> future = new CompletableFuture<>();
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				NodeMatcher<InheritanceNode> nodeMatcher = NodeMatcher.key(groupInheritanceNode);

				Map<UUID, Collection<InheritanceNode>> guildMemberSearchResults;
				guildMemberSearchResults = UM.searchAll(nodeMatcher).join();
				future.complete(new HashSet<>(guildMemberSearchResults.keySet()));
			} catch (Exception ex) {
				future.completeExceptionally(ex);
			}
		});
		return future;
	}

	// Gets the groups that inherit the group provided
	public static CompletableFuture<Set<String>> getGroupsInGroup(Group group) {
		return getGroupsInGroup(InheritanceNode.builder(group).build());
	}

	public static CompletableFuture<Set<String>> getGroupsInGroup(InheritanceNode groupInheritanceNode) {
		CompletableFuture<Set<String>> future = new CompletableFuture<>();
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				Map<String, Collection<InheritanceNode>> guildMemberSearchResults;
				NodeMatcher<InheritanceNode> nodeMatcher = NodeMatcher.key(groupInheritanceNode);
				guildMemberSearchResults = GM.searchAll(nodeMatcher).join();
				future.complete(new HashSet<>(guildMemberSearchResults.keySet()));
			} catch (Exception ex) {
				future.completeExceptionally(ex);
			}
		});
		return future;
	}

	public static CompletableFuture<Set<UUID>> getGuildInvites(Group guildRoot, GuildInviteLevel inviteLevel) {
		CompletableFuture<Set<UUID>> result = new CompletableFuture<>();
		if (inviteLevel.equals(GuildInviteLevel.NONE)) {
			result.complete(Set.of());
		}

		InheritanceNode inheritanceNode = InheritanceNode.builder(inviteLevel.groupNameFromRoot(guildRoot)).build();
		return getGroupMembers(inheritanceNode);
	}

	public static CompletableFuture<Set<UUID>> getAllGuildInvites(Group guildRoot) {
		CompletableFuture<Set<UUID>> result = new CompletableFuture<>();

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				Set<UUID> allInvites = new HashSet<>();
				for (GuildInviteLevel level : GuildInviteLevel.values()) {
					if (level.equals(GuildInviteLevel.NONE)) {
						continue;
					}

					Set<UUID> groupMembers = getGuildInvites(guildRoot, level).join();
					allInvites.addAll(groupMembers);
				}

				result.complete(allInvites);
			} catch (Exception ex) {
				result.completeExceptionally(ex);
			}
		});

		return result;
	}

	public static CompletableFuture<Set<UUID>> getGuildMembers(Group guildRoot, GuildAccessLevel accessLevel) {
		CompletableFuture<Set<UUID>> result = new CompletableFuture<>();
		if (accessLevel.equals(GuildAccessLevel.NONE)) {
			result.complete(Set.of());
		}

		InheritanceNode inheritanceNode = InheritanceNode.builder(accessLevel.groupNameFromRoot(guildRoot)).build();
		return getGroupMembers(inheritanceNode);
	}

	public static CompletableFuture<Set<UUID>> getAllGuildMembers(Group guildRoot, boolean skipGuests) {
		CompletableFuture<Set<UUID>> result = new CompletableFuture<>();

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				Set<UUID> allMembers = new HashSet<>();
				for (GuildAccessLevel level : GuildAccessLevel.values()) {
					if (level.equals(GuildAccessLevel.NONE)) {
						continue;
					}

					if (skipGuests && level.equals(GuildAccessLevel.GUEST)) {
						continue;
					}

					Set<UUID> groupMembers = getGuildMembers(guildRoot, level).join();
					allMembers.addAll(groupMembers);
				}

				result.complete(allMembers);
			} catch (Exception ex) {
				result.completeExceptionally(ex);
			}
		});

		return result;
	}

	public static Set<Player> getOnlineGuildInvites(Group guildRoot) {
		Set<Player> result = new HashSet<>();

		String inheritencePerm = "group." + GuildInviteLevel.GUEST_INVITE.groupNameFromRoot(guildRoot);
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.hasPermission(inheritencePerm)) {
				result.add(player);
			}
		}

		return result;
	}

	public static Set<Player> getOnlineGuildMembers(Group guildRoot, boolean skipGuests) {
		Set<Player> result = new HashSet<>();

		if (skipGuests) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				Group playerAccessLevel = getGuild(player);
				Group actualGuildRoot = getGuildRoot(playerAccessLevel);
				if (guildRoot.equals(actualGuildRoot)) {
					result.add(player);
				}
			}
		} else {
			String inheritencePerm = "group." + GuildAccessLevel.GUEST.groupNameFromRoot(guildRoot);
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (player.hasPermission(inheritencePerm)) {
					result.add(player);
				}
			}
		}

		return result;
	}

	public static String getGuildId(String plainGuildTag) {
		return "guild." + LuckPermsIntegration.getCleanLpString(plainGuildTag);
	}

	public static String getCleanLpString(String lpString) {
		// Guild name sanitization for command usage
		return lpString
			.toLowerCase(Locale.getDefault())
			.replaceAll("[\\[\\]{}().*]", "")
			.replace(" ", "_");
	}

	public static CompletableFuture<Boolean> setLocked(Group guild, boolean locked) {
		CompletableFuture<Boolean> result = new CompletableFuture<>();
		Group root = getGuildRoot(guild);
		if (root == null) {
			Bukkit.getScheduler().runTask(Plugin.getInstance(),
				() -> result.completeExceptionally(new Exception("Guild not found!")));
			return result;
		}

		List<MetaNode> toDelete = new ArrayList<>();
		for (MetaNode node : root.getNodes(NodeType.META)) {
			if (node.getMetaKey().equals(GUILD_ROOT_LOCKDOWN_MK)) {
				toDelete.add(node);
			}
		}
		for (MetaNode node : toDelete) {
			root.data().remove(node);
		}

		if (locked) {
			root.data().add(MetaNode.builder(GUILD_ROOT_LOCKDOWN_MK, "true").build());
		}

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				GM.saveGroup(root).join();
				pushUpdate();
				result.complete(locked);
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> result.completeExceptionally(ex));
			}
		});
		return result;
	}

	public static boolean isLocked(Group guild) {
		Group root = getGuildRoot(guild);
		if (root == null) {
			return true;
		}

		for (MetaNode node : root.getNodes(NodeType.META)) {
			if (node.getMetaKey().equals(GUILD_ROOT_LOCKDOWN_MK)) {
				return true;
			}
		}

		return false;
	}

	// runSync: if true, run method on the thread it's called from
	public static void setGuildBanner(@Nullable CommandSender sender,
	                                  Group guild,
	                                  Plugin plugin,
	                                  ItemStack guildBanner,
	                                  boolean runSync) {
		if (sender == null) {
			sender = Bukkit.getConsoleSender();
		}
		Group group = getGuildRoot(guild);
		if (group == null) {
			sender.sendMessage(Component.text("Cannot set guild banner on non-guild permission group",
				NamedTextColor.RED));
			return;
		}
		ItemStack guildBannerTemplate = guildBanner;
		if (!ItemUtils.isBanner(guildBannerTemplate)) {
			sender.sendMessage(Component.text("Cannot set non-banner as guild banner",
				NamedTextColor.RED));
			return;
		}
		ItemMeta templateMeta = guildBannerTemplate.getItemMeta();

		guildBanner = new ItemStack(guildBannerTemplate.getType());
		ItemMeta meta = guildBanner.getItemMeta();
		if (templateMeta instanceof BannerMeta templateBannerMeta
			&& meta instanceof BannerMeta bannerMeta) {
			bannerMeta.setPatterns(templateBannerMeta.getPatterns());
		}
		guildBanner.setItemMeta(meta);

		String guildBannerStr = ItemUtils.serializeItemStack(guildBanner);

		// Remove all the other banner meta nodes
		List<MetaNode> toDelete = new ArrayList<>();
		for (MetaNode node : group.getNodes(NodeType.META)) {
			if (node.getMetaKey().startsWith(GUILD_ROOT_BANNER_MK)) {
				toDelete.add(node);
			}
		}
		for (MetaNode node : toDelete) {
			group.data().remove(node);
		}

		int bannerStrLength = guildBannerStr.length();
		int numParts = 0;
		for (int startIndex = 0; startIndex < bannerStrLength; startIndex += META_VALUE_SECTION_SIZE) {
			int endIndex = Math.min(startIndex + META_VALUE_SECTION_SIZE, bannerStrLength);
			group.data().add(MetaNode.builder(GUILD_ROOT_BANNER_MK + META_KEY_NODE_SEPARATOR + numParts,
				guildBannerStr.substring(startIndex, endIndex)).build());
			numParts++;
		}
		group.data().add(MetaNode.builder(GUILD_ROOT_BANNER_LENGTH_MK, String.valueOf(bannerStrLength)).build());

		if (runSync) {
			GM.saveGroup(group).join();
			pushUpdate();
		} else {
			CommandSender finalSender = sender;
			Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
				try {
					GM.saveGroup(group).join();
					pushUpdate();
				} catch (Throwable ex) {
					Bukkit.getScheduler().runTask(plugin, () -> {
						finalSender.sendMessage(Component.text("Failed to set guild banner:",
							NamedTextColor.RED));
						MessagingUtils.sendStackTrace(finalSender, ex);
					});
				}
			});
		}
	}

	public static ItemStack getGuildBanner(Group guild) {
		Group group = getGuildRoot(guild);
		if (group == null) {
			ItemStack notAGuild = new ItemStack(Material.PLAYER_HEAD);
			SkullMeta meta = (SkullMeta) notAGuild.getItemMeta();
			meta.setPlayerProfile(ERROR_QUESTION_MARK_PLAYER_PROFILE);
			meta.displayName(Component.text("[no guild banner set]",
					NamedTextColor.RED,
					TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			notAGuild.setItemMeta(meta);
			return notAGuild;
		}
		MetaNode lengthMetaNode = null;
		Map<String, MetaNode> sectionMetaNodes = new HashMap<>();
		for (MetaNode node : group.getNodes(NodeType.META)) {
			String metaKey = node.getMetaKey();
			if (metaKey.equals(GUILD_ROOT_BANNER_LENGTH_MK)) {
				lengthMetaNode = node;
			} else if (metaKey.startsWith(GUILD_ROOT_BANNER_MK)) {
				sectionMetaNodes.put(metaKey, node);
			}
		}
		if (lengthMetaNode == null) {
			ItemStack errorItem = new ItemStack(Material.BARRIER);
			ItemMeta meta = errorItem.getItemMeta();
			meta.displayName(Component.text("[banner data size not found]", NamedTextColor.RED));
			List<Component> lore = List.of(Component.text("Banner item length could not be found.",
				NamedTextColor.RED));
			meta.lore(lore);
			errorItem.setItemMeta(meta);
			return errorItem;
		}

		int bannerStrLength;
		try {
			bannerStrLength = Integer.parseInt(lengthMetaNode.getMetaValue());
		} catch (Exception ex) {
			MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);

			ItemStack exceptionItem = new ItemStack(Material.BARRIER);
			ItemMeta meta = exceptionItem.getItemMeta();
			meta.displayName(Component.text("[an error occurred getting the guild banner]"));
			List<Component> lore = new ArrayList<>();
			lore.add(Component.text(ex.getMessage(), NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false));
			meta.lore(lore);
			exceptionItem.setItemMeta(meta);
			return exceptionItem;
		}

		StringBuilder bannerStrBuilder = new StringBuilder();
		int rebuiltLength = 0;
		for (int partIndex = 0; rebuiltLength < bannerStrLength; partIndex++) {
			String partId = GUILD_ROOT_BANNER_MK + META_KEY_NODE_SEPARATOR + partIndex;
			MetaNode partNode = sectionMetaNodes.get(partId);
			if (partNode == null) {
				ItemStack errorItem = new ItemStack(Material.BARRIER);
				ItemMeta meta = errorItem.getItemMeta();
				meta.displayName(Component.text("[could not get banner item part]", NamedTextColor.RED));
				List<Component> lore = List.of(Component.text("Banner item part not found: index " + partIndex,
					NamedTextColor.RED));
				meta.lore(lore);
				errorItem.setItemMeta(meta);
				return errorItem;
			}
			String partStr = partNode.getMetaValue();
			rebuiltLength += partStr.length();
			bannerStrBuilder.append(partStr);
		}

		try {
			return ItemUtils.parseItemStack(bannerStrBuilder.toString());
		} catch (Exception ex) {
			MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);

			ItemStack exceptionItem = new ItemStack(Material.BARRIER);
			ItemMeta meta = exceptionItem.getItemMeta();
			meta.displayName(Component.text("[an error occurred getting the guild banner]"));
			List<Component> lore = new ArrayList<>();
			lore.add(Component.text(ex.getMessage(), NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false));
			meta.lore(lore);
			exceptionItem.setItemMeta(meta);
			return exceptionItem;
		}
	}

	public static @Nullable ItemStack getGuildBanner(Player player) {
		Group guild = getGuild(player);
		if (guild == null) {
			return null;
		}
		return getGuildBanner(guild);
	}

	public static ItemStack getNonNullGuildBanner(Player player) {
		Group guild = getGuild(player);
		if (guild == null) {
			ItemStack item = new ItemStack(Material.BLUE_BANNER);
			ItemMeta meta = item.getItemMeta();
			if (meta instanceof BannerMeta bannerMeta) {
				// https://www.planetminecraft.com/banner/the-earth-408589/
				bannerMeta.addPattern(new Pattern(DyeColor.LIME, PatternType.GLOBE));
				bannerMeta.addPattern(new Pattern(DyeColor.GREEN, PatternType.GLOBE));
				bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.CURLY_BORDER));
				bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.BORDER));
				bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM));
				bannerMeta.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
			}
			item.setItemMeta(meta);
			return item;
		}
		return getGuildBanner(guild);
	}

	// This method is not to change the color/plain tag but to re-create and set the colored version.
	public static CompletableFuture<Void> rebuildTag(CommandSender sender, Group rootGuild) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			Group memberGroup = GuildAccessLevel.MEMBER.loadGroupFromRoot(rootGuild).join().orElse(null);

			if (memberGroup == null) {
				throw new IllegalStateException("Guild '" + rootGuild.getName() + "' is missing its member group.");
			}

			String plainTag = getGuildPlainTag(rootGuild);
			TextColor color = getGuildColor(rootGuild);

			if (plainTag == null || color == null) {
				throw new IllegalStateException("Guild '" + rootGuild.getName() + "' is missing either a plain tag or color");
			}

			String tag = color.asHexString() + "[" + plainTag + "]";

			NodeMap guildData = rootGuild.data();
			NodeMap memberData = memberGroup.data();

			for (Node node : guildData.toCollection()) {
				if (!(node instanceof MetaNode meta)) {
					continue;
				}

				if (meta.getMetaKey().equals(GUILD_ROOT_TAG_MK)) {
					guildData.remove(node);
				}
			}

			for (Node node : memberData.toCollection()) {
				if (node instanceof PrefixNode) {
					memberData.remove(node);
				}
			}

			guildData.add(MetaNode.builder(GUILD_ROOT_TAG_MK, tag).build());
			GM.saveGroup(rootGuild);

			memberData.add(PrefixNode.builder(tag, 1).build());
			GM.saveGroup(memberGroup);

			AuditListener.log("<+> Rebuilt tag of '" + rootGuild.getName() + "'.");
			future.complete(null);
		});
		return future;
	}

	public static CompletableFuture<GroupChildrenAndMembers> getGroupChildrenAndMembers(String permissionGroup, boolean recursive) {
		return getGroupChildrenAndMembers(new HashSet<>(), permissionGroup, recursive);
	}

	private static CompletableFuture<GroupChildrenAndMembers> getGroupChildrenAndMembers(
		Set<String> seen,
		String permissionGroup,
		boolean recursive
	) {
		Plugin plugin = Plugin.getInstance();
		CompletableFuture<GroupChildrenAndMembers> future = new CompletableFuture<>();

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				GroupChildrenAndMembers result = new GroupChildrenAndMembers(permissionGroup);
				InheritanceNode inheritanceNode = InheritanceNode.builder(permissionGroup).build();
				seen.add(permissionGroup);

				result.mMembers.addAll(getGroupMembers(inheritanceNode).join());

				Set<String> childGroups = new TreeSet<>(getGroupsInGroup(inheritanceNode).join());
				for (String childId : childGroups) {
					if (seen.contains(childId)) {
						continue;
					}
					if (recursive) {
						GroupChildrenAndMembers childResult
							= getGroupChildrenAndMembers(seen, childId, true).join();
						result.mChildGroups.put(childId, childResult);
					} else {
						result.mChildGroups.put(childId, new GroupChildrenAndMembers(childId));
					}
				}

				future.complete(result);
			} catch (Exception ex) {
				future.completeExceptionally(ex);
			}
		});

		return future;
	}

	public static CompletableFuture<Void> listGroupMembers(Audience audience, String permissionGroup, boolean recursive) {
		Plugin plugin = Plugin.getInstance();
		CompletableFuture<Void> future = new CompletableFuture<>();

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				GroupChildrenAndMembers groupChildrenAndMembers
					= getGroupChildrenAndMembers(permissionGroup, recursive).join();
				listGroupMembers(audience, groupChildrenAndMembers, 0, recursive);

				future.complete(null);
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(plugin, () -> {
					audience.sendMessage(Component.text(
						"Failed to delete " + permissionGroup + ":", NamedTextColor.RED, TextDecoration.BOLD));
					MessagingUtils.sendStackTrace(audience, ex);
				});
			}
		});

		return future;
	}

	private static CompletableFuture<Void> listGroupMembers(
		Audience audience,
		GroupChildrenAndMembers groupTree,
		int indentLevel,
		boolean recursive
	) {
		Plugin plugin = Plugin.getInstance();
		CompletableFuture<Void> future = new CompletableFuture<>();
		int nextLevel = indentLevel + 2;
		String outerIndent = "  ".repeat(indentLevel);
		String indent = "  ".repeat(indentLevel + 1);
		String innerIndent = "  ".repeat(nextLevel);

		audience.sendMessage(Component.text(outerIndent + "- Group " + groupTree.mName + ":", NamedTextColor.GOLD, TextDecoration.BOLD));

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				if (groupTree.mChildGroups.isEmpty()) {
					audience.sendMessage(Component.text(indent + "- No child groups", NamedTextColor.GOLD));
				} else {
					audience.sendMessage(Component.text(indent + "- Child groups:", NamedTextColor.GOLD));

					for (GroupChildrenAndMembers childTree : groupTree.mChildGroups.values()) {
						if (recursive) {
							listGroupMembers(audience, childTree, nextLevel, true).join();
							continue;
						}

						Optional<Group> optGroup = GM.loadGroup(childTree.mName).join();
						if (optGroup.isEmpty()) {
							audience.sendMessage(Component.text(innerIndent + "- Group " + groupTree.mName + " (could not load)", NamedTextColor.YELLOW, TextDecoration.BOLD));
							continue;
						}
						audience.sendMessage(Component.text(innerIndent + "- Group " + childTree.mName, NamedTextColor.GOLD));
					}
				}

				if (groupTree.mMembers.isEmpty()) {
					audience.sendMessage(Component.text(indent + "- No members in group " + groupTree.mName, NamedTextColor.YELLOW));
				} else {
					audience.sendMessage(Component.text(indent + "- Members: " + groupTree.mName, NamedTextColor.YELLOW));
					for (String memberName : PlayerUtils.sortedPlayerNamesFromUuids(groupTree.mMembers)) {
						audience.sendMessage(Component.text(innerIndent + "- " + memberName, NamedTextColor.GRAY));
					}
				}

				Optional<Group> optGroup = GM.loadGroup(groupTree.mName).join();
				if (optGroup.isEmpty()) {
					audience.sendMessage(Component.text(outerIndent + "- End of group " + groupTree.mName + ", which could not be loaded; assuming it was deleted.", NamedTextColor.YELLOW, TextDecoration.BOLD));
					future.complete(null);
					return;
				}
				audience.sendMessage(Component.text(outerIndent + "- End of group " + groupTree.mName, NamedTextColor.GOLD, TextDecoration.BOLD));

				future.complete(null);
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(plugin, () -> {
					audience.sendMessage(Component.text(outerIndent + "- Failed to list " + groupTree.mName + " due to an error:", NamedTextColor.RED, TextDecoration.BOLD));
					MessagingUtils.sendStackTrace(audience, ex);
					future.completeExceptionally(ex);
				});
			}
		});

		return future;
	}

	public static CompletableFuture<Void> renameGroup(Audience audience, String oldId, String newId) {
		Plugin plugin = Plugin.getInstance();
		CompletableFuture<Void> future = new CompletableFuture<>();

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				audience.sendMessage(Component.text("Checking existence of old/new group IDs...", NamedTextColor.BLUE, TextDecoration.BOLD));
				if (loadGroup(newId).join().isPresent()) {
					audience.sendMessage(Component.text("Cannot rename " + oldId
							+ " to " + newId
							+ " because " + newId
							+ " already exists! If this is after a failed rename, delete " + newId
							+ " and try renaming again.",
						NamedTextColor.RED));
					future.complete(null);
					return;
				}

				Optional<Group> optOldGroup = GM.loadGroup(oldId).join();
				if (optOldGroup.isEmpty()) {
					audience.sendMessage(Component.text("Cannot rename " + oldId
							+ " to " + newId
							+ " because " + oldId
							+ " does not exist! Create it again if you want to move existing members.",
						NamedTextColor.RED));
					future.complete(null);
					return;
				}

				audience.sendMessage(Component.text("Creating new group with identical permissions...", NamedTextColor.BLUE, TextDecoration.BOLD));
				Group oldGroup = optOldGroup.get();
				Group newGroup = GM.createAndLoadGroup(newId).join();

				NodeMap newData = newGroup.data();
				for (Node node : oldGroup.data().toCollection()) {
					newData.add(node);
				}
				GM.saveGroup(newGroup);
				pushUpdate();

				audience.sendMessage(Component.text("Adding old members and child groups to the new group...", NamedTextColor.BLUE, TextDecoration.BOLD));
				InheritanceNode newInheritanceNode = InheritanceNode.builder(newId).build();

				GroupChildrenAndMembers oldChildrenAndMembers
					= getGroupChildrenAndMembers(oldId, false).join();

				for (String childGroupId : oldChildrenAndMembers.mChildGroups.keySet()) {
					Optional<Group> optChildGroup = loadGroup(childGroupId).join();
					if (optChildGroup.isEmpty()) {
						audience.sendMessage(Component.text("- Unable to load/update child group " + childGroupId + ", skipping!", NamedTextColor.RED, TextDecoration.BOLD));
						continue;
					}

					Group childGroup = optChildGroup.get();
					childGroup.data().add(newInheritanceNode);
					GM.saveGroup(childGroup);
					pushUpdate();
				}

				for (UUID memberId : oldChildrenAndMembers.mMembers) {
					User member = loadUser(memberId).join();
					member.data().add(newInheritanceNode);
					pushUserUpdate(member);
				}

				audience.sendMessage(Component.text("Deleting the old group...", NamedTextColor.BLUE, TextDecoration.BOLD));
				deleteGroup(audience, oldChildrenAndMembers, 0, false).join();

				audience.sendMessage(Component.text("Renamed " + oldId + " to " + newId + "!", NamedTextColor.GREEN, TextDecoration.BOLD));

				future.complete(null);
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(plugin, () -> {
					audience.sendMessage(Component.text(
						"Failed to rename " + oldId + " to " + newId + ":", NamedTextColor.RED, TextDecoration.BOLD));
					MessagingUtils.sendStackTrace(audience, ex);
				});
			}
		});

		return future;
	}

	public static CompletableFuture<Void> deleteGroup(Audience audience, String permissionGroup, boolean recursive) {
		Plugin plugin = Plugin.getInstance();
		CompletableFuture<Void> future = new CompletableFuture<>();

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				GroupChildrenAndMembers groupChildrenAndMembers
					= getGroupChildrenAndMembers(permissionGroup, recursive).join();
				deleteGroup(audience, groupChildrenAndMembers, 0, recursive);

				future.complete(null);
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(plugin, () -> {
					audience.sendMessage(Component.text(
						"Failed to delete " + permissionGroup + ":", NamedTextColor.RED, TextDecoration.BOLD));
					MessagingUtils.sendStackTrace(audience, ex);
				});
			}
		});

		return future;
	}

	private static CompletableFuture<Void> deleteGroup(
		Audience audience,
		GroupChildrenAndMembers groupTree,
		int indentLevel,
		boolean recursive
	) {
		Plugin plugin = Plugin.getInstance();
		CompletableFuture<Void> future = new CompletableFuture<>();
		int nextLevel = indentLevel + 1;
		String outerIndent = "  ".repeat(indentLevel);
		String indent = "  ".repeat(nextLevel);
		String innerIndent = "  ".repeat(nextLevel + 1);

		audience.sendMessage(Component.text(outerIndent + "- Preparing to delete group " + groupTree.mName, NamedTextColor.GOLD, TextDecoration.BOLD));

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				InheritanceNode inheritanceNode = InheritanceNode.builder(groupTree.mName).build();

				if (groupTree.mChildGroups.isEmpty()) {
					if (recursive) {
						audience.sendMessage(Component.text(indent + "- No child groups to delete", NamedTextColor.GOLD));
					} else {
						audience.sendMessage(Component.text(indent + "- No child groups to remove from " + groupTree.mName, NamedTextColor.GOLD));
					}
				} else {
					if (!recursive) {
						audience.sendMessage(Component.text(indent + "- Removing child inheritance nodes", NamedTextColor.GOLD));
					}

					for (GroupChildrenAndMembers childTree : groupTree.mChildGroups.values()) {
						if (recursive) {
							deleteGroup(audience, childTree, nextLevel, true).join();
							continue;
						}

						Optional<Group> optGroup = GM.loadGroup(childTree.mName).join();
						if (optGroup.isEmpty()) {
							continue;
						}
						Group group = optGroup.get();
						group.data().remove(inheritanceNode);
						GM.saveGroup(group).join();
						pushUpdate();
						audience.sendMessage(Component.text(innerIndent + "- Removed child group " + childTree.mName + " from " + groupTree.mName, NamedTextColor.GOLD));
					}
				}

				if (groupTree.mMembers.isEmpty()) {
					audience.sendMessage(Component.text(indent + "- No members to remove from group " + groupTree.mName, NamedTextColor.YELLOW));
				} else {
					audience.sendMessage(Component.text(indent + "- Removing members from group " + groupTree.mName, NamedTextColor.YELLOW));
					for (UUID memberUuid : groupTree.mMembers) {
						User member = UM.loadUser(memberUuid).join();
						member.data().remove(inheritanceNode);
						pushUserUpdate(member);
					}
					for (String memberName : PlayerUtils.sortedPlayerNamesFromUuids(groupTree.mMembers)) {
						audience.sendMessage(Component.text(innerIndent + "- " + memberName, NamedTextColor.GRAY));
					}
				}

				Optional<Group> optGroup = GM.loadGroup(groupTree.mName).join();
				if (optGroup.isEmpty()) {
					audience.sendMessage(Component.text(outerIndent + "- Could not find group " + groupTree.mName + "; assuming it was already deleted.", NamedTextColor.YELLOW, TextDecoration.BOLD));
					future.complete(null);
					return;
				}
				Group group = optGroup.get();
				GM.deleteGroup(group).join();
				pushUpdate();
				audience.sendMessage(Component.text(outerIndent + "- Deleted group " + groupTree.mName + ".", NamedTextColor.GOLD, TextDecoration.BOLD));
				Bukkit.getScheduler().runTask(plugin,
					() -> AuditListener.log("<-> Deleted group '" + groupTree.mName + "'."));

				future.complete(null);
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(plugin, () -> {
					audience.sendMessage(Component.text(outerIndent + "- Failed to delete " + groupTree.mName + " due to an error:", NamedTextColor.RED, TextDecoration.BOLD));
					MessagingUtils.sendStackTrace(audience, ex);
					future.completeExceptionally(ex);
				});
			}
		});

		return future;
	}

	public static void pushUpdate() {
		Optional<MessagingService> mso = LP.getMessagingService();
		mso.ifPresent(MessagingService::pushUpdate);
	}

	public static void pushUserUpdate(User user) {
		UM.saveUser(user);
		Optional<MessagingService> mso = LP.getMessagingService();
		mso.ifPresent(messagingService -> messagingService.pushUserUpdate(user));
	}

	public static void updatePlayerGuildChat(Player player) {
		Group guild = getGuild(player);
		if (isModern(guild)) {
			String plainTag = getGuildPlainTag(guild);
			if (plainTag == null) {
				return;
			}
			Channel guildChannel = MonumentaNetworkChatIntegration.getChannel(plainTag);
			if (guildChannel != null) {
				MonumentaNetworkChatIntegration.setPlayerDefaultGuildChat(player, guildChannel);
			}
		} else {
			MonumentaNetworkChatIntegration.unsetPlayerDefaultGuildChat(player);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		updatePlayerGuildChat(player);
	}
}

package com.playmonumenta.plugins.integrations.luckperms.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.GuildInviteLevel;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.sync.PreNetworkSyncEvent;
import net.luckperms.api.event.user.UserLoadEvent;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.getLoadedUsers;

public class InviteNotification {
	private static final Map<UUID, Set<String>> mUserInvitesPreSync = new HashMap<>();

	@SuppressWarnings("resource")
	public static void registerLuckPermsEvents(Plugin plugin, EventBus eventBus) {
		eventBus.subscribe(plugin, NodeAddEvent.class, InviteNotification::nodeAddEvent);

		eventBus.subscribe(plugin, PreNetworkSyncEvent.class, InviteNotification::preNetworkSyncEvent);
		eventBus.subscribe(plugin, UserLoadEvent.class, InviteNotification::userLoadEvent);
	}

	public static void nodeAddEvent(NodeAddEvent event) {
		PermissionHolder holder = event.getTarget();

		if (!(holder instanceof User user)) {
			return;
		}

		Node node = event.getNode();
		if (!(node instanceof InheritanceNode inheritanceNode)) {
			return;
		}

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			Group group = LuckPermsIntegration.loadGroup(inheritanceNode.getGroupName()).join().orElse(null);
			if (group == null) {
				return;
			}

			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				Player player = Plugin.getInstance().getPlayer(user.getUniqueId());
				if (player == null) {
					return;
				}

				notifyInvited(player, group);
			});
		});
	}

	public static void preNetworkSyncEvent(PreNetworkSyncEvent event) {
		// Make sure these are empty.
		mUserInvitesPreSync.clear();

		MMLog.fine("[Guild Invite Listener/PreNetworkSync] Loading current user data");
		for (User user : getLoadedUsers()) {
			Set<String> userGroups = new HashSet<>();
			for (Group group : user.getInheritedGroups(QueryOptions.nonContextual())) {
				userGroups.add(group.getName());
			}
			mUserInvitesPreSync.put(user.getUniqueId(), userGroups);
		}
	}

	public static void userLoadEvent(UserLoadEvent event) {
		MMLog.fine("[Guild Invite Listener/UserLoad] Got event");
		if (mUserInvitesPreSync.isEmpty()) {
			return;
		}
		Map<UUID, Set<String>> userInvitesPreSync = new HashMap<>(mUserInvitesPreSync);
		mUserInvitesPreSync.clear();

		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			for (Player player : Bukkit.getOnlinePlayers()) {
				User user = LuckPermsIntegration.getUser(player);
				Set<String> oldUserGroups = userInvitesPreSync.get(user.getUniqueId());
				if (oldUserGroups == null) {
					// User was not online and may be ignored; handled by login listener
					continue;
				}

				MMLog.fine("[Guild Invite Listener/PostSync] - " + player.getName());
				for (Group group : user.getInheritedGroups(QueryOptions.nonContextual())) {
					MMLog.fine("[Guild Invite Listener/PostSync]   - " + group.getName());
					if (oldUserGroups.contains(group.getName())) {
						MMLog.fine("[Guild Invite Listener/PostSync]     - old, skip");
						continue;
					}

					notifyInvited(player, group);
				}
			}
		});
	}

	public static void notifyInvited(Player player, Group group) {
		Group guildRoot = LuckPermsIntegration.getGuildRoot(group);
		GuildInviteLevel inviteLevel = GuildInviteLevel.byGroup(group);
		if (guildRoot == null || inviteLevel.equals(GuildInviteLevel.NONE)) {
			return;
		}

		Component guildComponent = Component.text("", NamedTextColor.GOLD)
			.append(LuckPermsIntegration.getGuildFullComponent(guildRoot));
		player.sendMessage(Component.text("You have been invited to ", NamedTextColor.AQUA)
			.append(guildComponent)
			.append(Component.text(" as a " + inviteLevel.mArgument + ".")));
		if (inviteLevel.equals(GuildInviteLevel.MEMBER_INVITE)) {
			player.sendMessage(Component.text(
				"If you do not wish to become a member, you can accept the invite as a guest instead, or decline the invite.",
				NamedTextColor.GRAY));
		} else {
			player.sendMessage(Component.text(
				"If you do not wish to become a guest, you can decline the invite instead.",
				NamedTextColor.GRAY));
		}
		player.sendMessage(Component.text("[Check your invites]", NamedTextColor.LIGHT_PURPLE)
			.clickEvent(ClickEvent.runCommand("/guild gui accessible")));
	}

	public static void notifyInvitedLogin(Player player) {
		Set<Group> invites = LuckPermsIntegration.getRelevantGuilds(player, false, true);
		int memberInvites = 0;
		int guestInvites = 0;
		for (Group inviteGroup : invites) {
			GuildInviteLevel inviteLevel = GuildInviteLevel.byGroup(inviteGroup);
			if (inviteLevel.equals(GuildInviteLevel.MEMBER_INVITE)) {
				memberInvites++;
			} else if (inviteLevel.equals(GuildInviteLevel.GUEST_INVITE)) {
				guestInvites++;
			}
		}

		String memberPlural = memberInvites >= 2 ? "s" : "";
		String guestPlural = guestInvites >= 2 ? "s" : "";
		if (memberInvites <= 0 && guestInvites <= 0) {
			return;
		} else if (memberInvites > 0 && guestInvites > 0) {
			player.sendMessage(Component.text("You have been invited to ", NamedTextColor.AQUA)
				.append(Component.text(memberInvites + " guild" + memberPlural, NamedTextColor.GOLD))
				.append(Component.text(" as a member, and "))
				.append(Component.text(guestInvites + " guild" + guestPlural, NamedTextColor.GOLD))
				.append(Component.text(" as a guest.")));
			player.sendMessage(Component.text(
				"If you do not wish to become a member, you can accept the invite as a guest instead, or decline the invite.",
				NamedTextColor.GRAY));
			player.sendMessage(Component.text(
				"If you do not wish to become a guest, you can decline the invite instead.",
				NamedTextColor.GRAY));
		} else if (memberInvites > 0) {
			player.sendMessage(Component.text("You have been invited to ", NamedTextColor.AQUA)
				.append(Component.text(memberInvites + " guild" + memberPlural, NamedTextColor.GOLD))
				.append(Component.text(" as a member.")));
			player.sendMessage(Component.text(
				"If you do not wish to become a member, you can accept the invite as a guest instead, or decline the invite.",
				NamedTextColor.GRAY));
		} else {
			player.sendMessage(Component.text("You have been invited to ", NamedTextColor.AQUA)
				.append(Component.text(guestInvites + " guild" + guestPlural, NamedTextColor.GOLD))
				.append(Component.text(" as a guest.")));
			player.sendMessage(Component.text(
				"If you do not wish to become a guest, you can decline the invite instead.",
				NamedTextColor.GRAY));
		}
		player.sendMessage(Component.text("[Check your invites]", NamedTextColor.LIGHT_PURPLE)
			.clickEvent(ClickEvent.runCommand("/guild gui accessible")));
	}
}

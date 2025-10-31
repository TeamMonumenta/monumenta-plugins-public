package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.networkchat.commands.ChatCommand;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

public class GuildInviteCommand {
	private static final Argument<String> USER_ARG = new StringArgument("player").replaceSuggestions(ChatCommand.ALL_CACHED_PLAYER_NAMES_SUGGESTIONS);
	private static final CommandPermission PERMISSION = CommandPermission.fromString("monumenta.command.guild.invite");
	private static final CommandPermission PERMISSION_MOD = CommandPermission.fromString("monumenta.command.guild.mod.invite");

	private static final int MAX_INVITES = 4;

	public static CommandAPICommand attach(Plugin plugin, CommandAPICommand root, GuildInviteLevel inviteLevel) {
		// <playername> <invite level>

		return root
			.withArguments(USER_ARG)
			.withArguments(new LiteralArgument(inviteLevel.mArgument))
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, PERMISSION);
				CommandSender callee = sender;
				if (GuildCommand.senderCannotRunCommand(callee, false)) {
					throw CommandAPI.failWithString("You cannot run this command on 'build'");
				}

				if (callee instanceof ProxiedCommandSender proxied) {
					callee = proxied.getCallee();
				}
				if (!(callee instanceof Player inviter)) {
					throw CommandAPI.failWithString("This command may only be run by a player");
				}

				String stringUser = args.getUnchecked("player");
				runAsPlayer(plugin, inviter, stringUser, inviteLevel);
			});
	}

	public static CommandAPICommand attachModOnly(Plugin plugin, CommandAPICommand root, GuildInviteLevel inviteLevel) {
		// <guild> <playername> <new_access>

		return root
			.withArguments(GuildCommand.GUILD_NAME_ARG)
			.withArguments(USER_ARG)
			.withArguments(new LiteralArgument(inviteLevel.mArgument))
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, PERMISSION_MOD);
				CommandSender callee = sender;
				if (GuildCommand.senderCannotRunCommand(callee, true)) {
					throw CommandAPI.failWithString("You cannot run this command");
				}

				if (callee instanceof ProxiedCommandSender proxied) {
					callee = proxied.getCallee();
				}
				if (!(callee instanceof Player inviter)) {
					throw CommandAPI.failWithString("This command may only be run by a player");
				}

				String guildName = args.getUnchecked("guild name");
				String stringUser = args.getUnchecked("player");
				runAsMod(plugin, inviter, guildName, stringUser, inviteLevel);
			});
	}

	public static void runAsPlayer(Plugin plugin, Player inviter, String invitedName, GuildInviteLevel inviteLevel) {
		Group guildRoot = LuckPermsIntegration.getGuildRoot(LuckPermsIntegration.getGuild(inviter));
		if (guildRoot == null) {
			inviter.sendMessage(Component.text("You are not in a guild", NamedTextColor.RED));
			return;
		}
		if (!GuildPermission.MANAGE_MEMBERSHIP.hasAccess(guildRoot, inviter)) {
			inviter.sendMessage(Component.text("You do not have permission to manage invites for your guild.", NamedTextColor.RED));
			return;
		}
		if (!MetadataUtils.checkOnceInRecentTicks(Plugin.getInstance(), inviter, "Guild Invite", 10)) {
			inviter.sendMessage(Component.text("Slow down! You're sending invites too fast.", NamedTextColor.RED));
			return;
		}
		if (!MetadataUtils.checkOnceInRecentTicks(Plugin.getInstance(), inviter, "Guild Invite " + invitedName, 1200)) {
			inviter.sendMessage(Component.text("Don't spam! Please wait a minute and try again.", NamedTextColor.RED));
			return;
		}

		runSetInvite(plugin, inviter, invitedName, guildRoot, inviteLevel);
	}

	public static void runAsMod(Plugin plugin, CommandSender inviter, String guildName, String invitedName, GuildInviteLevel inviteLevel) {
		String guildId = GuildArguments.getIdFromName(guildName);
		if (guildId == null) {
			inviter.sendMessage(Component.text("Could not identify guild by the name " + guildName, NamedTextColor.RED));
			return;
		}

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				Group root = LuckPermsIntegration.getGuildRoot(LuckPermsIntegration.GM.loadGroup(guildId).join().orElse(null));
				if (root == null) {
					Bukkit.getScheduler().runTask(plugin,
						() -> inviter.sendMessage(Component.text("Could not find guild " + guildName, NamedTextColor.RED)));
					return;
				}

				runSetInvite(plugin, inviter, invitedName, root, inviteLevel);
			} catch (Throwable ex) {
				Bukkit.getScheduler().runTask(plugin, () -> {
					inviter.sendMessage(Component.text("An error occurred setting the invite level for " + invitedName, NamedTextColor.RED));
					MessagingUtils.sendStackTrace(inviter, ex);
				});
			}
		});
	}

	private static void runSetInvite(Plugin plugin, CommandSender inviter, String invitedName, Group guildRoot, GuildInviteLevel inviteLevel) {
		UUID invitedUuid = MonumentaRedisSyncIntegration.cachedNameToUuid(invitedName);
		if (invitedUuid == null) {
			inviter.sendMessage(Component.text("Could not get the player " + invitedName + "; have they joined the game before?", NamedTextColor.RED));
			return;
		}

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			User invitedUser = LuckPermsIntegration.loadUser(invitedUuid).join();
			if (invitedUser == null) {
				Bukkit.getScheduler().runTask(plugin,
					() -> inviter.sendMessage(Component.text("Could not get the LuckPerms data for " + invitedName, NamedTextColor.RED)));
				return;
			}

			try {
				Set<UUID> invitedPlayerIds = LuckPermsIntegration.getAllGuildInvites(guildRoot).join();

				if (!invitedPlayerIds.contains(invitedUuid) && invitedPlayerIds.size() >= MAX_INVITES) {
					Bukkit.getScheduler().runTask(plugin, () ->
						inviter.sendMessage(Component.text("Your guild may only invite " + MAX_INVITES + " players at once!", NamedTextColor.RED)));
					return;
				}

				GuildInviteLevel.setInviteLevel(invitedUser, guildRoot, inviteLevel).join();

				Bukkit.getScheduler().runTask(plugin, () -> {
					Component guildComponent = LuckPermsIntegration.getGuildFullComponent(guildRoot);
					switch (inviteLevel) {
						case MEMBER_INVITE ->
							inviter.sendMessage(Component.text("Invited " + invitedName + " to ", NamedTextColor.GOLD)
								.append(guildComponent)
								.append(Component.text(" as a member.")));
						case GUEST_INVITE ->
							inviter.sendMessage(Component.text("Invited " + invitedName + " to ", NamedTextColor.GOLD)
								.append(guildComponent)
								.append(Component.text(" as a guest.")));
						default ->
							inviter.sendMessage(Component.text("Uninvited " + invitedName + " from ", NamedTextColor.GOLD)
								.append(guildComponent));
					}
					AuditListener.logPlayer(
						"[Guild] Sent invite - inviter=" + inviter.getName()
							+ ", invited=" + invitedName
							+ ", guild=" + MessagingUtils.plainText(guildComponent)
							+ ", level=" + inviteLevel.mArgument
					);
				});
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(plugin, () -> {
					Component guildComponent = LuckPermsIntegration.getGuildFullComponent(guildRoot);
					switch (inviteLevel) {
						case MEMBER_INVITE ->
							inviter.sendMessage(Component.text("Failed to invite " + invitedName + " to ", NamedTextColor.GOLD)
								.append(guildComponent)
								.append(Component.text(" as a member.")));
						case GUEST_INVITE ->
							inviter.sendMessage(Component.text("Failed to invite " + invitedName + " to ", NamedTextColor.GOLD)
								.append(guildComponent)
								.append(Component.text(" as a guest.")));
						default ->
							inviter.sendMessage(Component.text("Failed to uninvite " + invitedName + " from ", NamedTextColor.GOLD)
								.append(guildComponent));
					}
					MessagingUtils.sendStackTrace(inviter, ex);
				});
			}
		});
	}
}

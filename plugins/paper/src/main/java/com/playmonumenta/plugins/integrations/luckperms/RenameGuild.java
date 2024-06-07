package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.networkchat.channel.Channel;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.TextArgument;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.MetaNode;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_MEMBER_GUILD_NAME_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_MEMBER_HOVER_PREFIX_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_MEMBER_ROOT_ID_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_MEMBER_ROOT_PLAIN_TAG_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_ROOT_ID_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_ROOT_NAME_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_ROOT_PLAIN_TAG_MK;
import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.getGuildPlainTag;

public class RenameGuild {
	private static final Argument<String> NEW_NAME_ARG = new TextArgument("new name")
		.replaceSuggestions(GuildArguments.NAME_SUGGESTIONS);
	private static final Argument<String> NEW_TAG_ARG = new TextArgument("new tag")
		.replaceSuggestions(GuildArguments.TAG_SUGGESTIONS);

	private static final CommandPermission PERMISSION = CommandPermission.fromString("monumenta.command.guild.mod.renameguild");

	@SuppressWarnings("DataFlowIssue")
	public static CommandAPICommand attach(Plugin plugin, CommandAPICommand rootCommand) {
		CommandAPICommand tagSubCommand = new CommandAPICommand("tag")
			.withArguments(List.of(
				GuildCommand.GUILD_NAME_ARG,
				NEW_TAG_ARG
			))
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, PERMISSION);
				// Either you can run the command or it errors out.
				if (GuildCommand.senderCannotRunCommand(sender, true)) {
					return;
				}

				String guildName = args.getByArgument(GuildCommand.GUILD_NAME_ARG);
				String newTag = args.getByArgument(NEW_TAG_ARG);

				if (newTag.length() > LuckPermsIntegration.MAX_TAG_LENGTH) {
					throw CommandAPI.failWithString("Guild tag cannot exceed " + LuckPermsIntegration.MAX_TAG_LENGTH + " characters");
				}

				runRenameTag(plugin, sender, guildName, newTag);
			});
		CommandAPICommand nameSubCommand = new CommandAPICommand("name")
			.withArguments(List.of(
				GuildCommand.GUILD_NAME_ARG,
				NEW_NAME_ARG
			))
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, PERMISSION);
				// Either you can run the command or it errors out.
				if (GuildCommand.senderCannotRunCommand(sender, true)) {
					return;
				}

				String guildName = args.getByArgument(GuildCommand.GUILD_NAME_ARG);
				String newGuildName = args.getByArgument(NEW_NAME_ARG);

				runRenameGuild(plugin, sender, guildName, newGuildName);
			});

		CommandAPICommand allSubCommand = new CommandAPICommand("all")
			.withArguments(List.of(
				GuildCommand.GUILD_NAME_ARG,
				NEW_NAME_ARG,
				NEW_TAG_ARG
			))
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, PERMISSION);
				// Either you can run the command or it errors out.
				if (GuildCommand.senderCannotRunCommand(sender, true)) {
					return;
				}

				String guildName = args.getByArgument(GuildCommand.GUILD_NAME_ARG);
				String newGuildName = args.getByArgument(NEW_NAME_ARG);
				String newTag = args.getByArgument(NEW_TAG_ARG);

				if (newTag.length() > LuckPermsIntegration.MAX_TAG_LENGTH) {
					throw CommandAPI.failWithString("Guild tag cannot exceed " + LuckPermsIntegration.MAX_TAG_LENGTH + " characters");
				}

				runRenameBoth(plugin, sender, guildName, newGuildName, newTag);
			});

		return rootCommand
			.withSubcommands(tagSubCommand, nameSubCommand, allSubCommand);
	}

	public static void runRenameTag(Plugin plugin, CommandSender sender, String guild, String newTag) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			Component res = renameTag(plugin, guild, newTag, sender).join();
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> sender.sendMessage(res));
		});
	}

	public static void runRenameGuild(Plugin plugin, CommandSender sender, String oldName, String newName) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			Component res = renameGuild(plugin, oldName, newName, sender).join();
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> sender.sendMessage(res));
		});
	}

	public static void runRenameBoth(Plugin plugin, CommandSender sender, String oldName, String newName, String newTag) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			Component tagRes = renameTag(plugin, oldName, newTag, sender).join();
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> sender.sendMessage(tagRes));
			Component nameRes = renameGuild(plugin, oldName, newName, sender).join();
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> sender.sendMessage(nameRes));
		});
	}

	public static CompletableFuture<Component> renameTag(Plugin plugin, String guildName, String newTag, CommandSender sender) {
		CompletableFuture<Component> future = new CompletableFuture<>();
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			if (MonumentaNetworkChatIntegration.hasChannel(newTag)) {
				future.complete(Component.text("Cannot change tag for '"
					+ guildName + "' as a chat channel already exists with its new tag.", NamedTextColor.RED));
				return;
			}

			// Get root ids
			String oldRootId = GuildArguments.getIdFromName(guildName);
			if (oldRootId == null) {
				future.complete(Component.text("Could not identify guild by name: '"
					+ guildName + "'.", NamedTextColor.RED));
				return;
			}

			Group oldRoot = LuckPermsIntegration.GM.loadGroup(oldRootId).join().orElse(null);
			if (oldRoot == null) {
				future.complete(Component.text("Cannot change tag for '"
					+ guildName + "' as this guild does not exist.", NamedTextColor.RED));
				return;
			}

			String newRootId = LuckPermsIntegration.getGuildId(newTag);
			if (LuckPermsIntegration.GM.loadGroup(newRootId).join().isPresent()) {
				future.complete(Component.text("Cannot change tag for '"
					+ guildName + "' as there is already a guild with the ID '" + newRootId + "'.", NamedTextColor.RED));
				return;
			}

			// Get old tag for later use
			String oldTag = getGuildPlainTag(oldRoot);
			if (oldTag == null) {
				future.complete(Component.text("Guild '" + guildName
						+ "' does not have a tag\nCannot update guild channel (possibly does not have an existing channel.)",
					NamedTextColor.RED));
				return;
			}

			// Rename groups
			for (GuildInviteLevel inviteLevel : GuildInviteLevel.values()) {
				if (inviteLevel.equals(GuildInviteLevel.NONE)) {
					continue;
				}

				String oldChildId = inviteLevel.groupNameFromRoot(oldRootId);
				String newChildId = inviteLevel.groupNameFromRoot(newRootId);
				try {
					LuckPermsIntegration.renameGroup(sender, oldChildId, newChildId).join();
				} catch (Exception ex) {
					future.complete(Component.text("Failed to rename the "
						+ inviteLevel.mId + " child group of '"
						+ guildName + "' due to an error.", NamedTextColor.RED));
					MessagingUtils.sendStackTrace(sender, ex);
					return;
				}
			}

			for (GuildAccessLevel accessLevel : GuildAccessLevel.values()) {
				if (accessLevel.equals(GuildAccessLevel.NONE)) {
					continue;
				}

				String oldChildId = accessLevel.groupNameFromRoot(oldRootId);
				String newChildId = accessLevel.groupNameFromRoot(newRootId);
				try {
					LuckPermsIntegration.renameGroup(sender, oldChildId, newChildId).join();
				} catch (Exception ex) {
					future.complete(Component.text("Failed to rename the "
						+ accessLevel.mId + " child group of '"
						+ guildName + "' due to an error.", NamedTextColor.RED));
					MessagingUtils.sendStackTrace(sender, ex);
					return;
				}
			}

			try {
				LuckPermsIntegration.renameGroup(sender, oldRootId, newRootId).join();
			} catch (Exception ex) {
				future.complete(Component.text("Failed to rename the root group of '"
					+ guildName + "' due to an error.", NamedTextColor.RED));
				MessagingUtils.sendStackTrace(sender, ex);
				return;
			}

			// Update required node values
			Set<String> metaKeysToRemove;

			Group newRoot = LuckPermsIntegration.loadGroup(newRootId).join().orElse(null);
			if (newRoot == null) {
				future.complete(Component.text("Cannot change tag for '"
					+ guildName + "' as the root group could not be loaded after it was renamed", NamedTextColor.RED));
				return;
			}

			String memberGroupId = GuildAccessLevel.MEMBER.groupNameFromRoot(newRootId);
			Group guildMemberGroup = LuckPermsIntegration.loadGroup(memberGroupId).join().orElse(null);
			if (guildMemberGroup == null) {
				future.complete(Component.text("Cannot change tag for '"
					+ guildName + "' as the member group could not be loaded after it was renamed", NamedTextColor.RED));
				return;
			}

			NodeMap guildRootGroupData = newRoot.data();
			NodeMap guildMemberGroupData = guildMemberGroup.data();

			metaKeysToRemove = Set.of(
				GUILD_ROOT_ID_MK,
				GUILD_ROOT_PLAIN_TAG_MK
			);
			for (Node node : guildRootGroupData.toCollection()) {
				if (node instanceof MetaNode metaNode) {
					if (metaKeysToRemove.contains(metaNode.getMetaKey())) {
						guildRootGroupData.remove(metaNode);
					}
				}
			}
			guildRootGroupData.add(MetaNode.builder(GUILD_ROOT_ID_MK, newRootId).build());
			guildRootGroupData.add(MetaNode.builder(GUILD_ROOT_PLAIN_TAG_MK, newTag).build());
			LuckPermsIntegration.GM.saveGroup(newRoot).join();

			metaKeysToRemove = Set.of(
				GUILD_MEMBER_ROOT_ID_MK,
				GUILD_MEMBER_ROOT_PLAIN_TAG_MK
			);
			for (Node node : guildMemberGroupData.toCollection()) {
				if (node instanceof MetaNode metaNode) {
					if (metaKeysToRemove.contains(metaNode.getMetaKey())) {
						guildMemberGroupData.remove(metaNode);
					}
				}
			}
			guildMemberGroupData.add(MetaNode.builder(GUILD_MEMBER_ROOT_ID_MK, newRootId).build());
			guildMemberGroupData.add(MetaNode.builder(GUILD_MEMBER_ROOT_PLAIN_TAG_MK, newTag).build());
			LuckPermsIntegration.GM.saveGroup(guildMemberGroup).join();

			try {
				LuckPermsIntegration.rebuildTag(sender, newRoot).join();
			} catch (IllegalStateException ex) {
				MMLog.warning("Could not rebuild tag for guild '" + newRootId + "' due to: ", ex);
				future.complete(Component.text("Failed to rebuild guild tag", NamedTextColor.RED));
				return;
			}

			// Push update
			LuckPermsIntegration.pushUpdate();

			// Update channel
			String chatPerm = GuildPermission.CHAT.guildPermissionString(newRoot);
			if (chatPerm == null) {
				future.complete(Component.text("Could not identify chat permission.",
					NamedTextColor.RED));
				return;
			}
			Channel channel = MonumentaNetworkChatIntegration.transferGuildChannel(sender, oldTag, newTag, chatPerm);
			if (channel == null) {
				future.complete(Component.text("Could not update guild channel as it does not exist.",
					NamedTextColor.RED));
				return;
			}

			AuditListener.log("<+> Renamed guild: " + guildName + "'s tag to: '" + newTag + "'\nTask Executed by " + sender.getName());
			future.complete(Component.text("Successfully changed " + guildName + "'s tag to " + newTag,
				NamedTextColor.GOLD));
		});
		return future;
	}

	public static CompletableFuture<Component> renameGuild(Plugin plugin, String oldName, String newName, CommandSender sender) {
		CompletableFuture<Component> future = new CompletableFuture<>();
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			if (oldName.equals(newName)) {
				future.complete(Component.text("New name is the same as the old name.", NamedTextColor.GOLD));
				return;
			}

			// Get root ids
			String guildRootId = GuildArguments.getIdFromName(oldName);
			if (guildRootId == null) {
				future.complete(Component.text("Cannot identify guild by name " + oldName, NamedTextColor.RED));
				return;
			}

			Group root = LuckPermsIntegration.GM.loadGroup(guildRootId).join().orElse(null);
			if (root == null) {
				future.complete(Component.text("Cannot rename '"
					+ oldName + "' as this guild does not seem to exist.", NamedTextColor.RED));
				return;
			}

			// Update required node values
			Set<String> metaKeysToRemove;

			NodeMap guildRootGroupData = root.data();
			metaKeysToRemove = Set.of(
				GUILD_ROOT_NAME_MK
			);
			for (Node node : guildRootGroupData.toCollection()) {
				if (node instanceof MetaNode metaNode) {
					if (metaKeysToRemove.contains(metaNode.getMetaKey())) {
						guildRootGroupData.remove(metaNode);
					}
				}
			}
			guildRootGroupData.add(MetaNode.builder(GUILD_ROOT_NAME_MK, newName).build());
			LuckPermsIntegration.GM.saveGroup(root).join();

			Optional<Group> optMemberGroup = GuildAccessLevel.MEMBER.loadGroupFromRoot(guildRootId).join();
			if (optMemberGroup.isEmpty()) {
				future.complete(Component.text("Failed to rename the root group of '"
					+ oldName + "', as its member group did not load.", NamedTextColor.RED));
				return;
			}
			Group guildMemberGroup = optMemberGroup.get();
			NodeMap guildMemberGroupData = guildMemberGroup.data();
			metaKeysToRemove = Set.of(
				GUILD_MEMBER_HOVER_PREFIX_MK,
				GUILD_MEMBER_GUILD_NAME_MK
			);
			for (Node node : guildMemberGroupData.toCollection()) {
				if (node instanceof MetaNode metaNode) {
					if (metaKeysToRemove.contains(metaNode.getMetaKey())) {
						guildMemberGroupData.remove(metaNode);
					}
				}
			}
			guildMemberGroupData.add(MetaNode.builder(GUILD_MEMBER_HOVER_PREFIX_MK, newName).build());
			guildMemberGroupData.add(MetaNode.builder(GUILD_MEMBER_GUILD_NAME_MK, newName).build());
			LuckPermsIntegration.GM.saveGroup(guildMemberGroup).join();

			LuckPermsIntegration.pushUpdate();

			AuditListener.log("<+> Renamed guild '" + oldName + "' to '" + newName + "'\nTask Executed by " + sender.getName());
			future.complete(Component.text("Successfully changed " + oldName + "'s name to " + newName,
				NamedTextColor.GOLD));
		});
		return future;
	}
}

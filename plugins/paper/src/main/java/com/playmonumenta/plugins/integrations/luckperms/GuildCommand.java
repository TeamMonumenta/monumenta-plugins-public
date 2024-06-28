package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.integrations.luckperms.listeners.InviteNotification;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.MMLog;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GuildCommand {
	public static void register(Plugin plugin) {
		CommandAPICommand root = new CommandAPICommand("guild");
		CommandAPICommand modOnly = new CommandAPICommand("mod");

		attach(root);
		attachModOnly(plugin, modOnly);
		modOnly.withSubcommands(
			ChangeGuildBanner.attach(plugin, new CommandAPICommand("setbanner")),
			ChangeGuildColor.attach(plugin, new CommandAPICommand("color")),
			DeleteGuildCommand.attach(plugin, new CommandAPICommand("delete")),
			RenameGuild.attach(plugin, new CommandAPICommand("rename")),
			GuildChatColorCommand.attachModOnly(plugin, new CommandAPICommand("chatcolor"))
		);

		for (GuildAccessLevel accessLevel : GuildAccessLevel.values()) {
			modOnly.withSubcommands(
				GuildAccessCommand.attachModOnly(plugin, new CommandAPICommand("access"), accessLevel)
			);
			root.withSubcommands(
				GuildAccessCommand.attach(plugin, new CommandAPICommand("access"), accessLevel)
			);
		}

		for (GuildInviteLevel inviteLevel : GuildInviteLevel.values()) {
			modOnly.withSubcommands(
				GuildInviteCommand.attachModOnly(plugin, new CommandAPICommand("invite"), inviteLevel)
			);
			root.withSubcommands(
				GuildInviteCommand.attach(plugin, new CommandAPICommand("invite"), inviteLevel)
			);
		}


		root.withSubcommands(
			GuildChatColorCommand.attach(plugin, new CommandAPICommand("chatcolor")),
			modOnly //register mod-only commands
		);
		root.register();
	}

	protected static final Argument<String> GUILD_NAME_ARG = new TextArgument("guild name")
		.replaceSuggestions(GuildArguments.NAME_SUGGESTIONS);
	private static final CommandPermission REBUILD_PERMISSION
		= CommandPermission.fromString("monumenta.command.guild.mod.rebuildtag");

	private static CommandAPICommand attach(CommandAPICommand root) {
		//used to attach a few small commands that don't need their own file.

		CommandAPICommand inviteCheckSub = new CommandAPICommand("invitecheck");
		inviteCheckSub
			.executes((sender, args) -> {
				if (senderCannotRunCommand(sender, false)) {
					return;
				}

				CommandSender callee = CommandUtils.getCallee(sender);
				if (!(callee instanceof Player player)) {
					throw CommandAPI.failWithString("This command must be run as a player");
				}

				InviteNotification.notifyInvitedLogin(player);
			});

		return root.withSubcommands(
			inviteCheckSub
		);
	}

	private static CommandAPICommand attachModOnly(Plugin plugin, CommandAPICommand root) {
		//used to attach a few small commands that don't need their own file.

		CommandAPICommand rebuildTagSub = new CommandAPICommand("rebuildtag");
		rebuildTagSub
			.withArguments(GUILD_NAME_ARG)
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, REBUILD_PERMISSION);
				if (senderCannotRunCommand(sender, true)) {
					return;
				}

				String guildName = (String) args[args.length - 1];
				String rootId = GuildArguments.getIdFromName(guildName);
				if (rootId == null) {
					throw CommandAPI.failWithString("Could not identify guild by name: " + guildName);
				}

				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
					Group guild = LuckPermsIntegration.GM.loadGroup(rootId).join().orElse(null);
					Group rootGuild = LuckPermsIntegration.getGuildRoot(guild);
					if (rootGuild == null) {
						Bukkit.getScheduler().runTask(plugin,
							() -> sender.sendMessage(Component.text("Could not rebuild tag, as guild '"
								+ rootId + "' does not exist.", NamedTextColor.RED)));
						return;
					}

					try {
						LuckPermsIntegration.rebuildTag(sender, rootGuild).join();
					} catch (IllegalStateException ex) {
						Bukkit.getScheduler().runTask(plugin, () -> {
							MMLog.warning("Could not rebuild tag of '" + rootId + "' due to: ", ex);
							sender.sendMessage(Component.text("Failed to rebuild tag for '"
								+ rootId + "'", NamedTextColor.RED));
						});
						return;
					}

					//push update
					LuckPermsIntegration.pushUpdate();
					Bukkit.getScheduler().runTask(plugin, () -> AuditListener.log("<+> Finished updating tag for '"
						+ rootId + "'\nTask executed by: " + sender.getName())
					);
				});
			});

		return root.withSubcommands(
			rebuildTagSub
		);
	}

	public static boolean senderCannotRunCommand(CommandSender sender, boolean needOp)
		throws WrapperCommandSyntaxException {
		if (ServerProperties.getShardName().equals("build")) {
			throw CommandAPI.failWithString("This command cannot be run on the build shard.");
		}

		if (!sender.isOp() && needOp) {
			throw CommandAPI.failWithString("This command may only be run by an operator.");
		}

		return false;
	}
}

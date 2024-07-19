package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.integrations.luckperms.listeners.InviteNotification;
import com.playmonumenta.plugins.integrations.luckperms.listeners.Lockdown;
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
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

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
	private static final CommandPermission GUILD_BOUNDS_PERMISSION
		= CommandPermission.fromString("monumenta.command.guild.mod.guildbounds");

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

				String guildName = args.getByArgument(GUILD_NAME_ARG);
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

		CommandAPICommand guildIslandTestSub = getGuildIslandTestSub(plugin);

		return root
			.withSubcommands(
				rebuildTagSub
			)
			.withSubcommands(
				guildIslandTestSub
			);
	}

	private static @NotNull CommandAPICommand getGuildIslandTestSub(Plugin plugin) {
		CommandAPICommand guildIslandTestSub = new CommandAPICommand("guildislandtest");
		guildIslandTestSub
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, GUILD_BOUNDS_PERMISSION);
				if (senderCannotRunCommand(sender, true)) {
					return;
				}

				if (!"plots".equals(ServerProperties.getShardName())) {
					throw CommandAPI.failWithString("This command only works on the plots shard.");
				}

				final World originalPlotsWorld = Bukkit.getWorlds().get(0);
				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
					List<Group> guilds = LuckPermsIntegration.getGuilds().join();
					int numGuilds = guilds.size();

					int guildNum = 0;
					boolean foundProblems = false;
					for (Group guild : guilds) {
						guildNum++;

						List<BoundingBox> boundingBoxes
							= Lockdown.getOriginalGuildPlotAndIslands(originalPlotsWorld, guild, false).join();
						int numGuildIslands = Math.max(boundingBoxes.size() - 1, 0);
						boolean isProblem = numGuildIslands > 1;
						foundProblems |= isProblem;

						String guildName = LuckPermsIntegration.getNonNullGuildName(guild);
						sender.sendMessage(Component.text("- " + guildNum + "/" + numGuilds
								+ ": " + guildName + " has " + numGuildIslands + " guild island(s)",
							isProblem ? NamedTextColor.RED : NamedTextColor.GREEN));

						try {
							Thread.sleep(50);
						} catch (InterruptedException ignored) {
							// This delay reduces lag from loading chunks, getting interrupted is irrelevant
						}
					}

					sender.sendMessage(Component.text("Done!", foundProblems ? NamedTextColor.RED : NamedTextColor.GREEN));
				});
			});
		return guildIslandTestSub;
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

package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.ObjectiveArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.wrappers.NativeProxyCommandSender;
import java.util.Collection;
import net.luckperms.api.model.group.Group;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;

public class GuildTestCommand {
	private static final CommandPermission PERMISSION
		= CommandPermission.fromString("monumenta.command.guild.mech");
	private static final EntitySelectorArgument.ManyPlayers PLAYERS_ARG
		= new EntitySelectorArgument.ManyPlayers("players");
	private static final ObjectiveArgument OBJECTIVE_ARG = new ObjectiveArgument("objective");

	public static void attach(CommandAPICommand root) {
		root
			.withSubcommand(new CommandAPICommand("get_plot_id")
				.executesNative(GuildTestCommand::getPlotId))
			.withSubcommand(new CommandAPICommand("get_world_plot_id")
				.executesNative(GuildTestCommand::getWorldPlotId))
			.withSubcommand(new CommandAPICommand("mark_guild_mates")
				.withArguments(PLAYERS_ARG, OBJECTIVE_ARG)
				.executesNative(GuildTestCommand::markGuildMates))
		;

		for (GuildArgs guildArgs : GuildArgs.getArgVariants("", "")) {
			for (GuildAccessLevel accessLevel : GuildAccessLevel.values()) {
				if (GuildAccessLevel.NONE.equals(accessLevel)) {
					continue;
				}

				root
					.withSubcommand(new CommandAPICommand("test_access_level")
						.withArguments(new LiteralArgument(accessLevel.mArgument))
						.withArguments(guildArgs.getArgs())
						.executesNative((NativeProxyCommandSender sender, CommandArguments args)
							-> testGuildAccessLevel(sender, args, guildArgs, accessLevel)))
				;
			}

			for (GuildPermission guildPermission : GuildPermission.values()) {
				root
					.withSubcommand(new CommandAPICommand("test_guild_permission")
						.withArguments(new LiteralArgument(guildPermission.mArgument))
						.withArguments(guildArgs.getArgs())
						.executesNative((NativeProxyCommandSender sender, CommandArguments args)
							-> testGuildPermission(sender, args, guildArgs, guildPermission)))
				;
			}

			for (GuildFlag guildFlag : GuildFlag.values()) {
				root
					.withSubcommand(new CommandAPICommand("test_guild_flag")
						.withArguments(new LiteralArgument(guildFlag.mArgument))
						.withArguments(guildArgs.getArgs())
						.executesNative((NativeProxyCommandSender sender, CommandArguments args)
							-> testGuildFlag(sender, args, guildArgs, guildFlag)))
				;
			}
		}
	}

	public static int getPlotId(NativeProxyCommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
		CommandUtils.checkPerm(sender, PERMISSION);
		CommandSender callee = CommandUtils.getCallee(sender);
		if (!(callee instanceof Player player)) {
			return 0;
		}
		Group guild = LuckPermsIntegration.getGuild(player);
		Long plotId = LuckPermsIntegration.getGuildPlotId(guild);
		if (plotId == null) {
			return 0;
		}

		if (plotId < 0 || plotId > Integer.MAX_VALUE) {
			return -1;
		}

		return plotId.intValue();
	}

	public static int getWorldPlotId(NativeProxyCommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
		CommandUtils.checkPerm(sender, PERMISSION);

		Long plotId = GuildPlotUtils.getGuildPlotNumber(sender.getWorld());
		if (plotId == null) {
			return 0;
		}

		if (plotId < 0 || plotId > Integer.MAX_VALUE) {
			return -1;
		}

		return plotId.intValue();
	}

	@SuppressWarnings({"DataFlowIssue", "unchecked"})
	public static int markGuildMates(NativeProxyCommandSender sender, CommandArguments args) throws WrapperCommandSyntaxException {
		CommandUtils.checkPerm(sender, PERMISSION);

		Collection<Player> targetPlayers = args.getByArgument(PLAYERS_ARG);
		Objective markedObjective = args.getByArgument(OBJECTIVE_ARG);
		String markedObjectiveStr = markedObjective.getName();

		CommandSender callee = CommandUtils.getCallee(sender);
		if (!(callee instanceof Player mainPlayer)) {
			return 0;
		}
		Group guild = LuckPermsIntegration.getGuild(mainPlayer);
		Long plotId = LuckPermsIntegration.getGuildPlotId(guild);
		if (plotId == null || plotId == 0) {
			for (Player targetPlayer : targetPlayers) {
				ScoreboardUtils.setScoreboardValue(targetPlayer, markedObjectiveStr, 0);
			}
			return 0;
		}

		int matchCount = 0;
		for (Player targetPlayer : targetPlayers) {
			Group testGuild = LuckPermsIntegration.getGuild(targetPlayer);
			Long testPlotId = LuckPermsIntegration.getGuildPlotId(testGuild);

			if (plotId.equals(testPlotId)) {
				matchCount++;
				ScoreboardUtils.setScoreboardValue(targetPlayer, markedObjectiveStr, 1);
			} else {
				ScoreboardUtils.setScoreboardValue(targetPlayer, markedObjectiveStr, 0);
			}
		}

		return matchCount;
	}

	public static int testGuildAccessLevel(NativeProxyCommandSender sender, CommandArguments args, GuildArgs guildArgs, GuildAccessLevel testAccessLevel) throws WrapperCommandSyntaxException {
		CommandUtils.checkPerm(sender, PERMISSION);
		CommandSender callee = CommandUtils.getCallee(sender);
		if (!(callee instanceof Player player)) {
			return 0;
		}

		Group guild = guildArgs.getGuild(sender, args);
		if (guild == null) {
			return 0;
		}

		GuildAccessLevel accessLevel = LuckPermsIntegration.getAccessLevel(guild, LuckPermsIntegration.getUser(player));
		return accessLevel.compareTo(testAccessLevel) <= 0 ? 1 : 0;
	}

	public static int testGuildPermission(NativeProxyCommandSender sender, CommandArguments args, GuildArgs guildArgs, GuildPermission guildPermission) throws WrapperCommandSyntaxException {
		CommandUtils.checkPerm(sender, PERMISSION);
		CommandSender callee = CommandUtils.getCallee(sender);
		if (!(callee instanceof Player player)) {
			return 0;
		}

		Group guild = guildArgs.getGuild(sender, args);
		if (guild == null) {
			return 0;
		}

		return guildPermission.hasAccess(guild, player) ? 1 : 0;
	}

	public static int testGuildFlag(NativeProxyCommandSender sender, CommandArguments args, GuildArgs guildArgs, GuildFlag guildFlag) throws WrapperCommandSyntaxException {
		CommandUtils.checkPerm(sender, PERMISSION);

		Group guild = guildArgs.getGuild(sender, args);
		if (guild == null) {
			return 0;
		}

		return guildFlag.hasFlag(guild) ? 1 : 0;
	}
}

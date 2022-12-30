package com.playmonumenta.plugins.integrations.luckperms;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.luckperms.api.model.group.Group;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.Nullable;

public class TeleportGuild {
	private static final String COMMAND = "teleportguild";

	@SuppressWarnings("unchecked")
	public static void register() {
		// teleportguild <guildname> <player>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.teleportguild");

		List<Argument> arguments = new ArrayList<>();

		arguments.add(new EntitySelectorArgument("player", EntitySelector.MANY_PLAYERS));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				for (Player player : (List<Player>)args[0]) {
					run(player, null);
				}
			})
			.register();

		arguments.add(new TextArgument("guild name"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				for (Player player : (List<Player>)args[0]) {
					run(player, (String)args[1]);
				}
			})
			.register();
	}

	private static void run(Player player, @Nullable String guildName) throws WrapperCommandSyntaxException {

		Group group = null;

		if (guildName == null) {
			// Look up the player's guild
			group = LuckPermsIntegration.getGuild(player);
			if (group == null) {
				String err = ChatColor.RED + "You are not in a guild!";
				player.sendMessage(err);
				CommandAPI.fail(err);
				throw new RuntimeException();
			}
		} else {
			// need to look up from name

			// Guild name sanitization for command usage
			//TODO: Better lookup of guild name?
			String cleanGuildName = LuckPermsIntegration.getCleanGuildName(guildName);

			group = LuckPermsIntegration.GM.getGroup(cleanGuildName);
			if (group == null) {
				CommandAPI.fail("The luckperms group '" + cleanGuildName + "' does not exist");
				throw new RuntimeException();
			}
		}

		Location loc = LuckPermsIntegration.getGuildTp(player.getWorld(), group);

		if (loc == null) {
			player.sendMessage(ChatColor.RED + "The teleport for your guild is not set up");
			player.sendMessage(ChatColor.RED + "Please ask a moderator to fix this");
		} else {
			player.teleport(loc, PlayerTeleportEvent.TeleportCause.COMMAND);
		}
	}
}

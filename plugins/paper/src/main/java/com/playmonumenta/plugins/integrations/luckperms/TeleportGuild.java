package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.utils.CommandUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.Nullable;

public class TeleportGuild {
	private static final String COMMAND = "teleportguild";

	@SuppressWarnings("unchecked")
	public static void register() {
		// teleportguild <player> [<guildname>]
		CommandPermission perms = CommandPermission.fromString("monumenta.command.teleportguild");

		List<Argument<?>> arguments = new ArrayList<>();

		arguments.add(new EntitySelectorArgument.ManyPlayers("player"));
		new CommandAPICommand(COMMAND)
			.withArguments(arguments)
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, perms);
				for (Player player : (List<Player>)args[0]) {
					run(player, null);
				}
			})
			.register();

		arguments.add(new TextArgument("guild name")
			.replaceSuggestions(GuildArguments.NAME_SUGGESTIONS));
		new CommandAPICommand(COMMAND)
			.withArguments(arguments)
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, perms);
				for (Player player : (List<Player>)args[0]) {
					run(player, (String)args[1]);
				}
			})
			.register();
	}

	private static void run(Player player, @Nullable String guildName) {
		World world = player.getWorld();
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			Group group;

			if (guildName == null) {
				// Look up the player's guild
				group = LuckPermsIntegration.getGuild(player);
				if (group == null) {
					Component err = Component.text("You are not in a guild", NamedTextColor.RED);
					player.sendMessage(err);
					return;
				}
			} else {
				// need to look up from name

				// Guild name sanitization for command usage
				String guildId = GuildArguments.getIdFromName(guildName);
				if (guildId == null) {
					player.sendMessage(Component.text("Could not identify guild by name '" + guildName
						+ "'", NamedTextColor.RED));
					return;
				}

				group = LuckPermsIntegration.GM.loadGroup(guildId).join().orElse(null);
				if (group == null) {
					player.sendMessage(Component.text("The luckperms group '" + guildId
						+ "' does not exist", NamedTextColor.RED));
					return;
				}
			}

			String actualGuildName = LuckPermsIntegration.getNonNullGuildName(group);

			if (LuckPermsIntegration.isLocked(group)) {
				if (player.isOp()) {
					player.sendMessage(Component.text("The guild " + actualGuildName
						+ " is locked, but your operator status lets you bypass this.", NamedTextColor.GOLD));
				} else {
					Component err = Component.text("The guild " + actualGuildName
						+ " is locked. A moderator will need to unlock the guild first.", NamedTextColor.RED);
					player.sendMessage(err);
					return;
				}
			}

			Optional<Location> optLoc = LuckPermsIntegration.getGuildTp(world, group).join();
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				if (optLoc.isEmpty()) {
					player.sendMessage(Component.text("The teleport for your guild is not set up", NamedTextColor.RED));
					player.sendMessage(Component.text("Please ask a moderator to fix this", NamedTextColor.RED));
					return;
				}

				player.teleport(optLoc.get(), PlayerTeleportEvent.TeleportCause.COMMAND);
			});
		});
	}
}

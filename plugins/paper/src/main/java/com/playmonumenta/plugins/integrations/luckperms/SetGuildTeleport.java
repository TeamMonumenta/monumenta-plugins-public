package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.CommandUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetGuildTeleport {
	public static void register(Plugin plugin) {
		// setguildteleport <guildname>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.setguildteleport");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new TextArgument("guild name")
			.replaceSuggestions(GuildArguments.NAME_SUGGESTIONS));

		new CommandAPICommand("setguildteleport")
			.withArguments(arguments)
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, perms);
				if (ServerProperties.getShardName().contains("build")) {
					throw CommandAPI.failWithString("This command cannot be run on the build shard.");
				}

				run(plugin, sender, (String) args[0]);
			})
			.register();
	}

	private static void run(Plugin plugin, CommandSender sender,
	                        String guildName) throws WrapperCommandSyntaxException {

		if (!(sender instanceof Player player)) {
			throw CommandAPI.failWithString("This command can only be run by players");
		}

		// Guild name sanitization for command usage
		String cleanGuildName = GuildArguments.getIdFromName(guildName);
		if (cleanGuildName == null) {
			throw CommandAPI.failWithString("Could not identify guild by name " + guildName);
		}

		Location loc = player.getLocation();

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			Group group = LuckPermsIntegration.GM.loadGroup(cleanGuildName).join().orElse(null);
			if (group == null) {
				sender.sendMessage(Component.text("The luckperms group '" + cleanGuildName
					+ "' does not exist", NamedTextColor.RED));
				return;
			}

			LuckPermsIntegration.setGuildTp(sender, group, plugin, loc);

			player.sendMessage(Component.text("Guild teleport set to your location", NamedTextColor.GOLD));
		});
	}
}

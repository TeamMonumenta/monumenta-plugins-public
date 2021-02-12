package com.playmonumenta.plugins.integrations.luckperms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.MessagingService;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.User;

public class LeaveGuild {
	public static void register(Plugin plugin) {

		// leaveguild <playername>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.leaveguild");

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));

		new CommandAPICommand("leaveguild")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				run(plugin, (Player) args[0]);
			})
			.register();
	}

	private static void run(Plugin plugin, Player player) throws WrapperCommandSyntaxException {
		// Set scores and permissions
		ScoreboardUtils.setScoreboardValue(player, "Founder", 0);

		for (Node userNode : LuckPermsIntegration.LP.getUser(player.getUniqueId()).getOwnNodes()) {
			if (userNode.isGroupNode()) {
				Group group = LuckPermsIntegration.LP.getGroup(userNode.getGroupName());
				boolean guildFound = false;
				String guildName = "";

				for (Node groupChildNode : group.getNodes().values()) {
					if (groupChildNode.isMeta()) {
						Entry<String, String> meta = groupChildNode.getMeta();
						if (meta.getKey().equals("guildname")) {
							guildName = meta.getValue();
							guildFound = true;
							break;
						}
					}
				}
				if (guildFound) {
					// Remove user from guild
					new BukkitRunnable() {
						@Override
						public void run() {
							User user = LuckPermsIntegration.LP.getUser(player.getUniqueId());
							user.unsetPermission(userNode);
							LuckPermsIntegration.LP.getUserManager().saveUser(user);
							LuckPermsIntegration.LP.runUpdateTask();
							LuckPermsIntegration.LP.getMessagingService().ifPresent(MessagingService::pushUpdate);
						}
					}.runTaskAsynchronously(plugin);

					player.sendMessage(ChatColor.GOLD + "You have left the guild '" + guildName + "'");
					return;
				}
			}
		}

		String err = ChatColor.RED + "You are not in a guild";
		player.sendMessage(err);
		CommandAPI.fail(err);
	}
}

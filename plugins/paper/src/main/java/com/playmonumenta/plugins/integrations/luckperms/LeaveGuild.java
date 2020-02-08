package com.playmonumenta.plugins.integrations.luckperms;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;

import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.MessagingService;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.User;

public class LeaveGuild {
	public static void register(Plugin plugin, LuckPermsApi lp) {

		// leaveguild <playername>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.leaveguild");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));

		CommandAPI.getInstance().register("leaveguild", perms, arguments, (sender, args) -> {
			run(plugin, lp, sender, (Player) args[0]);
		});
	}

	private static void run(Plugin plugin, LuckPermsApi lp, CommandSender sender, Player player) throws CommandSyntaxException {
		// Set scores and permissions
		ScoreboardUtils.setScoreboardValue(player, "Founder", 0);

		for (Node userNode : lp.getUser(player.getUniqueId()).getOwnNodes()) {
			if (userNode.isGroupNode()) {
				Group group = lp.getGroup(userNode.getGroupName());
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
							User user = lp.getUser(player.getUniqueId());
							user.unsetPermission(userNode);
							lp.getUserManager().saveUser(user);
							lp.runUpdateTask();
							lp.getMessagingService().ifPresent(MessagingService::pushUpdate);
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

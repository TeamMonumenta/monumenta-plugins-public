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
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;

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

		Group group = LuckPermsIntegration.getGuild(player);
		if (group == null) {
			String err = ChatColor.RED + "You are not in a guild";
			player.sendMessage(err);
			CommandAPI.fail(err);
		}

		String guildName = LuckPermsIntegration.getGuildName(group);

		new BukkitRunnable() {
			@Override
			public void run() {
				User user = LuckPermsIntegration.UM.getUser(player.getUniqueId());
				for (InheritanceNode node : user.getNodes(NodeType.INHERITANCE)) {
					if (node.getGroupName().equals(group.getName())) {
						user.data().remove(node);
					}
				}
				LuckPermsIntegration.UM.saveUser(user);
				LuckPermsIntegration.pushUserUpdate(user);
			}
		}.runTaskAsynchronously(plugin);

		player.sendMessage(ChatColor.GOLD + "You have left the guild '" + guildName + "'");
	}
}

package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class JoinGuild {
	public static void register(Plugin plugin) {
		// joinguild <playername>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.joinguild");

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument("player", EntitySelector.MANY_PLAYERS));

		new CommandAPICommand("joinguild")
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((founder, args) -> {
				if (!ServerProperties.getShardName().contains("build")) {
					run(plugin, founder, (List<Player>) args[0]);
				}
			})
			.register();
	}

	private static void run(Plugin plugin, Player founder, List<Player> players) throws WrapperCommandSyntaxException {
		Group currentGuild = LuckPermsIntegration.getGuild(founder);
		String currentGuildName = LuckPermsIntegration.getGuildName(currentGuild);
		if (currentGuildName != null &&
			ScoreboardUtils.getScoreboardValue(founder, "Founder").orElse(0) != 1) {
			String err = ChatColor.RED + "You are not a founder of '" + currentGuildName + "' !";
			CommandAPI.fail(err);
			return;
		}
		if (currentGuildName == null) {
			founder.sendMessage(ChatColor.RED + "You are not currently in a guild.");
			return;
		}
		players.removeIf(player -> founder.getName().equalsIgnoreCase(player.getName()));
		if (players.size() == 0) {
			founder.sendMessage(ChatColor.RED + "No other players found on the pedestal to add to your guild.");
		}

		// Check nearby players, add if not in guild and not founder of something
		for (Player p : players) {
			if (ScoreboardUtils.getScoreboardValue(p, "Founder").orElse(0) == 0) {
				Group group = LuckPermsIntegration.getGuild(p);
				if (group != null) {
					p.sendMessage(ChatColor.RED + "You are already a part of another guild, please leave your current guild before trying again.");
					continue;
				}
				// Add user to guild
				new BukkitRunnable() {
					@Override
					public void run() {
						User user = LuckPermsIntegration.UM.getUser(p.getUniqueId());
						user.data().add(InheritanceNode.builder(currentGuild).build());
						LuckPermsIntegration.UM.saveUser(user).whenComplete((unused, ex) -> {
							if (ex != null) {
								ex.printStackTrace();
							}
						});
						LuckPermsIntegration.pushUserUpdate(user);
					}
				}.runTaskAsynchronously(plugin);

				// Success indicators
				p.sendMessage(ChatColor.GOLD + "Congratulations! You have joined " + currentGuildName + "!");
				founder.sendMessage(ChatColor.WHITE + p.getName() + ChatColor.GOLD + " has joined your guild");
				MonumentaNetworkChatIntegration.refreshPlayer(p);
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
					"execute at " + p.getName()
						+ " run summon minecraft:firework_rocket ~ ~1 ~ "
						+ "{LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:1,Colors:[I;16528693],FadeColors:[I;16777215]}]}}}}");
			} else {
				Group group = LuckPermsIntegration.getGuild(p);
				if (group != null) {
					p.sendMessage(ChatColor.RED + "You are marked as a founder but have no current guild, please contact a moderator.");
					continue;
				}
				p.sendMessage(ChatColor.RED + "You are the founder of another guild, please leave your current guild before trying again.");
			}
		}
	}
}

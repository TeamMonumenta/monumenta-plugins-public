package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

public class PromoteGuild {
	@SuppressWarnings("unchecked")
	public static void register() {
		// promoteguild <player collection>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.promoteguild");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.ManyPlayers("players"));

		new CommandAPICommand("promoteguild")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				CommandSender callee = sender;
				if (callee instanceof ProxiedCommandSender proxiedCommandSender) {
					callee = proxiedCommandSender.getCallee();
				}
				if (callee instanceof Player founder) {
					if (!ServerProperties.getShardName().contains("build")) {
						run(founder, (List<Player>) args[0]);
					}
				} else {
					callee.sendMessage(Component.text("This command may only be run as a player.", NamedTextColor.RED));
				}
			})
			.register();
	}

	private static void run(Player founder, List<Player> players) throws WrapperCommandSyntaxException {
		//get data off the founder, confirm founder
		Group currentGuild = LuckPermsIntegration.getGuild(founder);
		String currentGuildName = LuckPermsIntegration.getGuildName(currentGuild);
		if (currentGuild == null || currentGuildName == null) {
			String err = ChatColor.RED + "Founder is not in a guild";
			throw CommandAPI.failWithString(err);
		}

		if (ScoreboardUtils.getScoreboardValue(founder, "Founder").orElse(0) != 1) {
			String err = ChatColor.RED + "You are not a founder of guild '" + currentGuildName + "'";
			throw CommandAPI.failWithString(err);
		}
		players.removeIf(player -> founder.getName().equalsIgnoreCase(player.getName()));
		if (players.size() == 0) {
			founder.sendMessage(ChatColor.RED + "No other players found on the pedestal to promote to founder.");
		}

		// Check the nearby players for proper setup
		for (Player p : players) {
			Group nearbyPlayerGroup = LuckPermsIntegration.getGuild(p);
			String nearbyPlayerGroupName = LuckPermsIntegration.getGuildName(nearbyPlayerGroup);
			if (nearbyPlayerGroup != null && nearbyPlayerGroupName != null &&
			    nearbyPlayerGroupName.equalsIgnoreCase(currentGuildName) &&
				ScoreboardUtils.getScoreboardValue(p, "Founder").orElse(0) == 0) {

				// Set scores and permissions
				ScoreboardUtils.setScoreboardValue(p, "Founder", 1);

				// Flair (mostly stolen from CreateGuild)
				p.sendMessage(ChatColor.GOLD + "Congratulations! You are now a founder of " + currentGuildName + "!");
				founder.sendMessage(ChatColor.WHITE + p.getName() + ChatColor.GOLD + " has been promoted to guild founder");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
					"execute at " + p.getName()
						+ " run summon minecraft:firework_rocket ~ ~1 ~ "
						+ "{LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:1,Colors:[I;16528693],FadeColors:[I;16777215]}]}}}}");
			} else if (ScoreboardUtils.getScoreboardValue(p, "Founder").orElse(0) == 1) {
				founder.sendMessage(p.getName() + ChatColor.GOLD + " is already a founder!");
			}
		}
	}
}

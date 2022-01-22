package com.playmonumenta.plugins.integrations.luckperms;

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
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PromoteGuild {
	public static void register() {
		// promoteguild <playername>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.promoteguild");

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));

		new CommandAPICommand("promoteguild")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				if (!ServerProperties.getShardName().contains("build")) {
					run((Player) args[0]);
				}
			})
			.register();
	}

	private static void run(Player player) throws WrapperCommandSyntaxException {
		Group currentGuild = LuckPermsIntegration.getGuild(player);
		String currentGuildName = LuckPermsIntegration.getGuildName(currentGuild);
		if (currentGuild == null || currentGuildName == null) {
			String err = ChatColor.RED + "You are not in a guild";
			player.sendMessage(err);
			CommandAPI.fail(err);
		}

		if (ScoreboardUtils.getScoreboardValue(player, "Founder").orElse(0) == 1) {
			String err = ChatColor.RED + "You are already a founder of guild '" + currentGuildName + "'";
			player.sendMessage(err);
			CommandAPI.fail(err);
		}

		// Check for nearby founder
		for (Player p : PlayerUtils.otherPlayersInRange(player, 1, true)) {
			Group nearbyPlayerGroup = LuckPermsIntegration.getGuild(p);
			String nearbyPlayerGroupName = LuckPermsIntegration.getGuildName(nearbyPlayerGroup);
			if (nearbyPlayerGroup != null && nearbyPlayerGroupName != null &&
			    nearbyPlayerGroupName.equalsIgnoreCase(currentGuildName) &&
				ScoreboardUtils.getScoreboardValue(p, "Founder").orElse(0) == 1) {

				// Set scores and permissions
				ScoreboardUtils.setScoreboardValue(player, "Founder", 1);

				// Flair (mostly stolen from CreateGuild)
				player.sendMessage(ChatColor.GOLD + "Congratulations! You are now a founder of " + currentGuildName + "!");
				p.sendMessage(ChatColor.WHITE + player.getName() + ChatColor.GOLD + " has been promoted to guild founder");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
				                       "execute at " + player.getName()
				                       + " run summon minecraft:firework_rocket ~ ~1 ~ "
				                       + "{LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:1,Colors:[I;16528693],FadeColors:[I;16777215]}]}}}}");
				// All done
				return;
			}
		}

		String err = ChatColor.RED + "A founder for " + currentGuildName + " needs to stand within 1 block of you";
		player.sendMessage(err);
		CommandAPI.fail(err);
	}
}

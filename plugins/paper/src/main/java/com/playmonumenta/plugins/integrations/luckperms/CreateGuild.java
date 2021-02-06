package com.playmonumenta.plugins.integrations.luckperms;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.concurrent.Executor;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import me.lucko.luckperms.api.MessagingService;
import me.lucko.luckperms.api.User;

public class CreateGuild {

	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		// createguild <guildname> <guild tag>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.createguild");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("guild name", new TextArgument());
		arguments.put("guild tag", new TextArgument());
		arguments.put("founders", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS)
		              .overrideSuggestions("@a[x=-770,y=106,z=-128,dx=7,dy=4,dz=13]"));

		new CommandAPICommand("createguild")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				run(plugin, sender, (String) args[0], (String) args[1], (Collection<Player>) args[2]);
			})
			.register();
	}

	private static void run(Plugin plugin, CommandSender sender,
	                        String guildName, String guildTag, Collection<Player> founders) throws WrapperCommandSyntaxException {

		// Guild name sanitization for command usage
		String cleanGuildName = LuckPermsIntegration.getCleanGuildName(guildName);

		//TODO: Better lookup of guild name?
		if (LuckPermsIntegration.LP.getGroup(cleanGuildName) != null) {
			CommandAPI.fail("The luckperms group '" + cleanGuildName + "' already exists!");
		}

		int totalPrestige = 0;
		boolean hasEnoughPrestige = true;
		boolean inGuildAlready = false;
		for (Player founder : founders) {
			if (LuckPermsIntegration.getGuild(founder) != null) {
				sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.ITALIC + "Player "
				                   + founder.getName() + " is already in a guild!");
				inGuildAlready = true;
			}

			int prestige = ScoreboardUtils.getScoreboardValue(founder, "Prestige");
			if (prestige < 8) {
				sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.ITALIC + "The minimal prestige count for "
				                   + founder.getName() + " is not reached ("
				                   + ScoreboardUtils.getScoreboardValue(founder, "Prestige") + "/8)");
				hasEnoughPrestige = false;
			}
			totalPrestige += prestige;
		}

		// Displays ALL founders in a guild / without enough prestige
		if (inGuildAlready) {
			CommandAPI.fail("At least one founder is already in a guild");
		}
		if (!hasEnoughPrestige) {
			CommandAPI.fail("Individual founder prestige requirements not met");
		}
		if (totalPrestige < 50) {
			CommandAPI.fail(ChatColor.DARK_RED + "" + ChatColor.ITALIC + "The total prestige count is not enough ("
			                   + totalPrestige + "/50)");
		}

		// Add tags, display messages and effects
		for (Player founder : founders) {
			ScoreboardUtils.setScoreboardValue(founder, "Founder", 1);
			founder.sendMessage(ChatColor.GOLD + "Congratulations! You have founded a new guild!");
			founder.playSound(founder.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1.5f);

			// fireworks!
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute at " + founder.getName()
			                       + " run summon minecraft:firework_rocket ~ ~1 ~ "
			                       + "{LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:1,Colors:[I;16528693],FadeColors:[I;16777215]}]}}}}");
		}

		try {
			MonumentaNetworkRelayIntegration.broadcastCommand("tellraw @a [\"\",{\"text\":\"A new guild has just been founded. Say hello to " + guildName + "!!\",\"bold\":true}]");
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Broadcasting command failed");
		}

		// Sort out permissions
		Executor executor = runnable -> Bukkit.getScheduler().runTask(plugin, runnable);
		LuckPermsIntegration.LP.getGroupManager().createAndLoadGroup(cleanGuildName).thenAcceptAsync(
			group -> {
				group.setPermission(LuckPermsIntegration.LP.getNodeFactory().makePrefixNode(1, guildTag).build());
				group.setPermission(LuckPermsIntegration.LP.getNodeFactory().makeMetaNode("hoverprefix", guildName).build());
				group.setPermission(LuckPermsIntegration.LP.getNodeFactory().makeMetaNode("guildname", guildName).build());
				for (Player founder : founders) {
					User user = LuckPermsIntegration.LP.getUser(founder.getUniqueId());
					user.setPermission(LuckPermsIntegration.LP.getNodeFactory().makeGroupNode(group).build());
					LuckPermsIntegration.LP.getUserManager().saveUser(user);
				}
				LuckPermsIntegration.LP.getGroupManager().saveGroup(group);
				LuckPermsIntegration.LP.runUpdateTask();
				LuckPermsIntegration.LP.getMessagingService().ifPresent(MessagingService::pushUpdate);
			}, executor
		);
	}
}

package com.playmonumenta.plugins.integrations.luckperms;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.concurrent.Executor;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.NetworkUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.TextArgument;

import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.MessagingService;
import me.lucko.luckperms.api.User;

public class CreateGuild {

	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin, LuckPermsApi lp) {
		// createguild <guildname> <guild tag>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.createguild");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("guild name", new TextArgument());
		arguments.put("guild tag", new TextArgument());
		arguments.put("founders", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS)
		              .overrideSuggestions("@a[x=-770,y=106,z=-128,dx=7,dy=4,dz=13]"));

		CommandAPI.getInstance().register("createguild", perms, arguments, (sender, args) -> {
			run(plugin, lp, sender, (String) args[0], (String) args[1], (Collection<Player>) args[2]);
		});
	}

	private static void run(Plugin plugin, LuckPermsApi lp, CommandSender sender,
	                        String guildName, String guildTag, Collection<Player> founders) throws CommandSyntaxException{

		// Guild name sanitization for command usage
		String cleanGuildName = LuckPermsIntegration.getCleanGuildName(guildName);

		if (lp.getGroup(cleanGuildName) != null) {
			CommandAPI.fail("The luckperms group '" + cleanGuildName + "' already exists!");
		}

		int totalPrestige = 0;
		boolean hasEnoughPrestige = true;
		boolean inGuildAlready = false;
		for (Player founder : founders) {
			if (LuckPermsIntegration.getGuild(lp, founder) != null) {
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
			founder.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD
			                    + "Congratulations! You have founded a new guild!");
			founder.playSound(founder.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1.5f);

			// fireworks!
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute at " + founder.getName()
			                       + " run summon minecraft:firework_rocket ~ ~1 ~ "
			                       + "{LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:1,Colors:[I;16528693],FadeColors:[I;16777215]}]}}}}");
		}

		try {
			NetworkUtils.broadcastCommand(plugin, "tellraw @a [\"\",{\"text\":\"A new guild has just been founded. Say hello to " + guildName + "!!\",\"bold\":true}]");
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Broadcasting command failed");
		}

		// Sort out permissions
		Executor executor = runnable -> Bukkit.getScheduler().runTask(plugin, runnable);
		lp.getGroupManager().createAndLoadGroup(cleanGuildName).thenAcceptAsync(
			group -> {
				group.setPermission(lp.getNodeFactory().makePrefixNode(1, guildTag).build());
				group.setPermission(lp.getNodeFactory().makeMetaNode("hoverprefix", guildName).build());
				group.setPermission(lp.getNodeFactory().makeMetaNode("guildname", guildName).build());
				for (Player founder : founders) {
					User user = lp.getUser(founder.getUniqueId());
					user.setPermission(lp.getNodeFactory().makeGroupNode(group).build());
					lp.getUserManager().saveUser(user);
				}
				lp.getGroupManager().saveGroup(group);
				lp.runUpdateTask();
				lp.getMessagingService().ifPresent(MessagingService::pushUpdate);
			}, executor
		);
	}
}

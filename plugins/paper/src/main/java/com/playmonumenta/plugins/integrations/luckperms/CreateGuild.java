package com.playmonumenta.plugins.integrations.luckperms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PrefixNode;

public class CreateGuild {

	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		// createguild <guildname> <guild tag>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.createguild");

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new TextArgument("guild name"));
		arguments.add(new TextArgument("guild tag"));
		arguments.add(new EntitySelectorArgument("founders", EntitySelector.MANY_PLAYERS)
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
		if (LuckPermsIntegration.GM.getGroup(cleanGuildName) != null) {
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

			int prestige = ScoreboardUtils.getScoreboardValue(founder, "Prestige").orElse(0);
			if (prestige < 8) {
				sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.ITALIC + "The minimal prestige count for "
				                   + founder.getName() + " is not reached ("
				                   + ScoreboardUtils.getScoreboardValue(founder, "Prestige").orElse(0) + "/8)");
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
		LuckPermsIntegration.GM.createAndLoadGroup(cleanGuildName).thenAcceptAsync(
			group -> {
				group.data().add(PrefixNode.builder(guildTag, 1).build());
				group.data().add(MetaNode.builder("hoverprefix", guildName).build());
				group.data().add(MetaNode.builder("guildname", guildName).build());
				for (Player founder : founders) {
					User user = LuckPermsIntegration.UM.getUser(founder.getUniqueId());
					user.data().add(InheritanceNode.builder(group).build());
					LuckPermsIntegration.UM.saveUser(user);
				}
				LuckPermsIntegration.GM.saveGroup(group);
				LuckPermsIntegration.pushUpdate();
			}, executor
		);
	}
}

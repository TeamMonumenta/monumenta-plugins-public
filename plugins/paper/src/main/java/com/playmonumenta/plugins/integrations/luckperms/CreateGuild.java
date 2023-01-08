package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.Component;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateGuild {
	private static final String[] SUGGESTIONS = {"@a[x=-770,y=106,z=-128,dx=7,dy=4,dz=13]"};

	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		// createguild <guildname> <guild tag>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.createguild");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new TextArgument("guild name"));
		arguments.add(new TextArgument("guild tag"));
		arguments.add(new EntitySelectorArgument.ManyPlayers("founders")
				.replaceSuggestions(ArgumentSuggestions.strings(info -> SUGGESTIONS)));

		new CommandAPICommand("createguild")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				if (!ServerProperties.getShardName().contains("build")) {
					run(plugin, sender, (String) args[0], (String) args[1], (Collection<Player>) args[2]);
				}
			})
			.register();
	}

	private static void run(Plugin plugin, CommandSender sender,
	                        String guildName, String guildTag, Collection<Player> founders) throws WrapperCommandSyntaxException {

		// Guild name sanitization for command usage
		String cleanGuildName = LuckPermsIntegration.getCleanGuildName(guildName);

		//TODO: Better lookup of guild name?
		if (LuckPermsIntegration.GM.getGroup(cleanGuildName) != null) {
			throw CommandAPI.failWithString("The luckperms group '" + cleanGuildName + "' already exists!");
		}

		boolean hasEnoughLevels = true;
		boolean inGuildAlready = false;
		for (Player founder : founders) {
			if (LuckPermsIntegration.getGuild(founder) != null) {
				sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.ITALIC + "Player "
					                   + founder.getName() + " is already in a guild!");
				inGuildAlready = true;
			}

			int level = ScoreboardUtils.getScoreboardValue(founder, AbilityUtils.TOTAL_LEVEL).orElse(0);
			if (level < 5) {
				sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.ITALIC + "The minimal level for "
					                   + founder.getName() + " is not reached ("
					                   + level + "/5)");
				hasEnoughLevels = false;
			}
		}

		// Displays ALL founders in a guild / without enough levels
		if (inGuildAlready) {
			throw CommandAPI.failWithString("At least one founder is already in a guild");
		}
		if (!hasEnoughLevels) {
			throw CommandAPI.failWithString("Individual founder level requirements not met");
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
					if (user == null) {
						sender.sendMessage(Component.text("No such luckperms user " + founder.getName()));
						continue;
					}
					user.data().add(InheritanceNode.builder(group).build());
					LuckPermsIntegration.UM.saveUser(user).whenComplete((unused, ex) -> {
						if (ex != null) {
							MessagingUtils.sendStackTrace(sender, ex);
							ex.printStackTrace();
						}
					});
				}
				LuckPermsIntegration.GM.saveGroup(group).whenComplete((unused, ex) -> {
					if (ex != null) {
						MessagingUtils.sendStackTrace(sender, ex);
						ex.printStackTrace();
					}
				});
				LuckPermsIntegration.pushUpdate();
			}, executor
		).whenComplete((unused, ex) -> {
			if (ex != null) {
				MessagingUtils.sendStackTrace(sender, ex);
				ex.printStackTrace();
			} else {
				Bukkit.getScheduler().runTask(plugin, () -> {
					// Create guild chat channel
					MonumentaNetworkChatIntegration.createGuildChannel(guildTag, cleanGuildName);

					// Add tags, display messages and effects
					for (Player founder : founders) {
						ScoreboardUtils.setScoreboardValue(founder, "Founder", 1);
						founder.sendMessage(ChatColor.GOLD + "Congratulations! You have founded a new guild!");
						founder.playSound(founder.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1.5f);

						// Refresh chat name
						MonumentaNetworkChatIntegration.refreshPlayer(founder);

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
				});
			}
		});
	}
}

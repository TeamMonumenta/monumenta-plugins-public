package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jaqobb.namemcapi.NameMCAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.FunctionArgument;
import dev.jorel.commandapi.arguments.ObjectiveArgument;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class NameMCVerify extends GenericCommand {
	public static void register(Plugin plugin) {
		new CommandAPICommand("namemcverify")
			.withPermission(CommandPermission.fromString("monumenta.command.namemcverify"))
			.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
			.withArguments(new ObjectiveArgument("objective"))
			.withArguments(new FunctionArgument("function"))
			.executes((sender, args) -> {
				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
					NameMCAPI api = new NameMCAPI();
					api.getServerRepository().cacheServer("server.playmonumenta.com", true, (server, ex) -> {
						Bukkit.getScheduler().runTask(plugin, () -> {
							Player player = (Player)args[0];
							if (ex != null || server == null) {
								player.sendMessage(ChatColor.RED + "Failed to get NameMC status, please try again. If this continues please report this bug");
								MessagingUtils.sendStackTrace(player, ex);
							} else {
								int val = server.hasLiked(player.getUniqueId()) ? 1 : 0;
								ScoreboardUtils.setScoreboardValue(player, (String)args[1], val);
								for (FunctionWrapper func : (FunctionWrapper[])args[2]) {
									func.run();
								}
							}
						});
					});
				});
			})
			.register();
	}
}

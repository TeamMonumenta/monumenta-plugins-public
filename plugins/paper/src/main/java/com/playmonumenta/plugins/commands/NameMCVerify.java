package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.FunctionArgument;
import dev.jorel.commandapi.arguments.ObjectiveArgument;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class NameMCVerify extends GenericCommand {
	public static void register(Plugin plugin) {
		new CommandAPICommand("namemcverify")
			.withPermission(CommandPermission.fromString("monumenta.command.namemcverify"))
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.withArguments(new ObjectiveArgument("objective"))
			.withArguments(new FunctionArgument("function"))
			.executes((sender, args) -> {
				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
					Player player = (Player)args[0];
					try {
						URL url = new URL("https://api.namemc.com/server/server.playmonumenta.com/likes?profile=" + player.getUniqueId());

						HttpURLConnection conn = (HttpURLConnection) url.openConnection();
						conn.setRequestMethod("GET");
						conn.connect();

						//Getting the response code
						int responsecode = conn.getResponseCode();
						if (responsecode != HttpURLConnection.HTTP_OK) {
							throw new Exception("NameMC Request failed : HTTP error code : " + responsecode);
						} else {
							try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
								StringBuilder response = new StringBuilder();
								String responseLine = null;
								while ((responseLine = br.readLine()) != null) {
									response.append(responseLine.trim());
								}
								MMLog.fine("Got request from NameMC API for player " + player.getName() + ": " + response.toString());

								Bukkit.getScheduler().runTask(plugin, () -> {
									int val = response.toString().equals("true") ? 1 : 0;
									ScoreboardUtils.setScoreboardValue(player, (String)args[1], val);
									for (FunctionWrapper func : (FunctionWrapper[])args[2]) {
										func.run();
									}
								});
							}
						}
					} catch (Exception e) {
						Bukkit.getScheduler().runTask(plugin, () -> {
							MMLog.fine("NameMC API request failed for player " + player.getName() + ": " + e.getMessage());
							player.sendMessage(Component.text("Failed to get NameMC status, please try again. If this continues please report this bug", NamedTextColor.RED));
						});
					}
				});
			})
			.register();
	}
}

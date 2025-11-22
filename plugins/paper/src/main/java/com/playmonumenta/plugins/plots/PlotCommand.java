package com.playmonumenta.plugins.plots;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.redissync.RBoardAPI;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration.ALL_CACHED_PLAYER_NAMES_SUGGESTIONS;
import static com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration.ALL_OTHER_CACHED_PLAYER_NAMES_SUGGESTIONS;

public class PlotCommand {
	private static final String COMMAND = "plot";
	private static final String SUBCOMMAND_HELP = "help";
	private static final String SUBCOMMAND_ADD = "add";
	private static final String SUBCOMMAND_REMOVE = "remove";
	private static final String SUBCOMMAND_INFO = "info";
	private static final String SUBCOMMAND_INFO_RAW = "info_raw";
	private static final String SUBCOMMAND_REGION = "region";
	private static final String SUBCOMMAND_ADD_OTHER = "add_other";
	private static final String SUBCOMMAND_REMOVE_OTHER = "remove_other";
	private static final String SUBCOMMAND_SEND = "send";
	private static final String SUBCOMMAND_GUI = "gui";
	private static final String SUBCOMMAND_BORDERGUI = "bordergui";
	private static final String SUBCOMMAND_NEW = "new";
	private static final String SUBCOMMAND_RESET = "reset";
	private static final String PERMISSION_ADD_OTHER = "monumenta.plots.addother";
	private static final String PERMISSION_REMOVE_OTHER = "monumenta.plots.removeother";
	private static final String PERMISSION_INFO_OTHER = "monumenta.plots.infoother";
	private static final String PERMISSION_SEND = "monumenta.plots.send";
	private static final String PERMISSION_GUI = "monumenta.plots.gui";
	private static final String PERMISSION_BORDERGUI = "monumenta.plots.bordergui";
	private static final String PERMISSION_NEW = "monumenta.plots.new";
	private static final String PERMISSION_RESET = "monumenta.plots.reset";
	private static final IntegerArgument REGION_ARGUMENT = new IntegerArgument("region", 0, 3);
	private static final MultiLiteralArgument MULTI_REGION_ARGUMENT = new MultiLiteralArgument("region", "all", "valley", "isles", "ring");

	@SuppressWarnings("unchecked")
	public static void register() {
		new CommandAPICommand(COMMAND)

			//region <PUBLIC COMMANDS>
			/* Public commands:
			- /plot help
			- /plot add <player> [duration]
			- /plot remove <player>
			- /plot info
			- /plot info_raw
			- /plot region <region>
			 */

			/* /plot help */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_HELP)
				.executesPlayer((sender, args) -> {
					plotHelp(sender);
				})
			)

			/* /plot add <player> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_ADD)
				// Suggest all online players except for the command sender and those who are vanished
				.withArguments(new StringArgument("player").replaceSuggestions(ALL_OTHER_CACHED_PLAYER_NAMES_SUGGESTIONS))
				.withOptionalArguments(new StringArgument("duration"))
				.executesPlayer((sender, args) -> {
					PlotManager.plotAccessAdd(sender, sender.getUniqueId(), StringUtils.getUuidFromInput((String) args.get("player")), (String) args.get("duration"));
				})
			)

			/* /plot remove <player> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_REMOVE)
				// Suggest all players with access to owner's plot
				.withArguments(new StringArgument("player")
					.replaceSuggestions(ArgumentSuggestions.stringsAsync((info) -> {
						if (!(info.sender() instanceof Player sender)) {
							// Return no suggestions if the command sender isn't a player
							return CompletableFuture.completedFuture(new String[0]);
						}

						return PlotManager.getOtherAccessesToOwnerPlot(sender.getUniqueId()).thenApply(otherAccesses ->
							otherAccesses.stream()
								.map(MonumentaRedisSyncIntegration::cachedUuidToName)
								.filter(Objects::nonNull)
								.toArray(String[]::new)
						);
					}))
				)
				.executesPlayer((sender, args) -> {
					PlotManager.plotAccessRemove(sender, (String) args.get("player"));
				})
			)

			/* /plot info */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_INFO)
				.executesPlayer((sender, args) -> {
					String senderName = sender.getName();
					UUID senderUuid = sender.getUniqueId();

					PlotManager.getPlotInfo(senderUuid).thenCompose(PlotManager.PlotInfo::populateNamesAndHeads).whenComplete((senderPlotInfo, ex) -> {
						if (ex != null) {
							MMLog.severe("Caught exception trying to list plot access for owner " + senderName + " : " + ex.getMessage());
							sender.sendMessage(Component.text("An error occurred while trying to list plot access. Please report this: " + ex.getMessage(), NamedTextColor.RED));
							MessagingUtils.sendStackTrace(sender, ex);
							return;
						}

						new PlotAccessGui(sender, senderName, senderUuid, senderPlotInfo, PlotAccessGui.MainGuiMode.ACCESS_INFO_MODE, Component.text("Plot Access Information")).open();
					});
				})
			)

			/* /plot info_raw */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_INFO_RAW)
				.executesPlayer((sender, args) -> {
					PlotManager.getPlotInfo(sender.getUniqueId()).whenComplete((senderPlotInfo, ex) -> {
						if (ex != null) {
							MMLog.severe("Caught exception trying to list plot access for owner " + sender.getName() + " : " + ex.getMessage());
							sender.sendMessage(Component.text("An error occurred while trying to list plot access. Please report this: " + ex.getMessage(), NamedTextColor.RED));
							MessagingUtils.sendStackTrace(sender, ex);
							return;
						}

						plotInfoRaw(sender, senderPlotInfo);
					});
				})
			)

			/* /plot region <region> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_REGION)
				.withArguments(REGION_ARGUMENT)
				.executesPlayer((sender, args) -> {
					int region = args.getByArgument(REGION_ARGUMENT);
					PlotManager.setPlotRegion(sender, region);
				}))
			.withSubcommand(new CommandAPICommand("region")
				.withArguments(MULTI_REGION_ARGUMENT)
				.executesPlayer((sender, args) -> {
					String regionString = args.getByArgument(MULTI_REGION_ARGUMENT);
					int region = switch (regionString) {
						case "all" -> 0;
						case "valley" -> 1;
						case "isles" -> 2;
						case "ring" -> 3;
						default -> 0;
					};
					PlotManager.setPlotRegion(sender, region);
				}))
			//endregion

			//region <MODERATOR COMMANDS>
			/* Moderator commands:
			- /plot add_other <plot owner> <other player> [duration]
			- /plot remove_other <plot owner> <other player>
			- /plot info <player>
			- /plot info_raw <player>
			- /plot send <players> [instance]
			- /plot gui <players>
			- /plot bordergui <players>
			- /plot new <players>
			- /plot reset <players>
			 */

			/* /plot add_other <plot owner> <other player> [duration] */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_ADD_OTHER)
				.withPermission(PERMISSION_ADD_OTHER)
				// Suggest names of every player to have joined the server for both arguments
				.withArguments(
					new StringArgument("plot owner").replaceSuggestions(ALL_CACHED_PLAYER_NAMES_SUGGESTIONS),
					new StringArgument("other player").replaceSuggestions(ALL_CACHED_PLAYER_NAMES_SUGGESTIONS)
				)
				.withOptionalArguments(new StringArgument("duration"))
				.executesPlayer((moderator, args) -> {
					UUID plotOwnerUUID = StringUtils.getUuidFromInput((String) args.get("plot owner"));
					UUID otherPlayerUUID = StringUtils.getUuidFromInput((String) args.get("other player"));
					PlotManager.plotAccessAdd(moderator, plotOwnerUUID, otherPlayerUUID, (String) args.get("duration"));
				})
			)

			/* /plot remove_other <plot owner> <other player> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_REMOVE_OTHER)
				.withPermission(PERMISSION_REMOVE_OTHER)
				.withArguments(
					// Suggest names of every player to have joined the server for the first argument
					new StringArgument("plot owner")
						.replaceSuggestions(ALL_CACHED_PLAYER_NAMES_SUGGESTIONS),
					// Suggest all players with access to previous name argument's plot for the second argument
					new StringArgument("other player")
						.replaceSuggestions(ArgumentSuggestions.stringsAsync((info) -> {
							String plotOwnerName = (String) info.previousArgs().get("plot owner");
							UUID plotOwnerUuid;

							try {
								plotOwnerUuid = StringUtils.getUuidFromInput(plotOwnerName);
							} catch (WrapperCommandSyntaxException ex) {
								// Return no suggestions if previous name argument is invalid
								return CompletableFuture.completedFuture(new String[0]);
							}

							return PlotManager.getOtherAccessesToOwnerPlot(plotOwnerUuid).thenApply(otherAccesses ->
								otherAccesses.stream()
									.map(MonumentaRedisSyncIntegration::cachedUuidToName)
									.filter(Objects::nonNull)
									.toArray(String[]::new)
							);
						}))
				)
				.executesPlayer((moderator, args) -> {
					UUID plotOwnerUUID = StringUtils.getUuidFromInput((String) args.get("plot owner"));
					UUID otherPlayerUUID = StringUtils.getUuidFromInput((String) args.get("other player"));
					PlotManager.plotAccessRemove(moderator, plotOwnerUUID, otherPlayerUUID);
				})
			)

			/* /plot info <player> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_INFO)
				// Suggest names of every player to have joined the server
				.withArguments(new StringArgument("player").replaceSuggestions(ALL_CACHED_PLAYER_NAMES_SUGGESTIONS)
					.withPermission(PERMISSION_INFO_OTHER))
				.executesPlayer((moderator, args) -> {
					String playerName = (String) args.get("player");
					UUID playerUuid = StringUtils.getUuidFromInput(playerName);

					PlotManager.getPlotInfo(playerUuid).thenCompose(PlotManager.PlotInfo::populateNamesAndHeads).whenComplete((playerPlotInfo, ex) -> {
						if (ex != null) {
							MMLog.severe("Caught exception trying to list plot access for owner " + playerName + " : " + ex.getMessage());
							moderator.sendMessage(Component.text("An error occurred while trying to list plot access. Please report this: " + ex.getMessage(), NamedTextColor.RED));
							MessagingUtils.sendStackTrace(moderator, ex);
							return;
						}

						new PlotAccessGui(moderator, playerName, playerUuid, playerPlotInfo, PlotAccessGui.MainGuiMode.ACCESS_INFO_MODE, Component.text("Plot Access Information")).open();
					});
				})
			)

			/* /plot info_raw <player> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_INFO_RAW)
				// Suggest names of every player to have joined the server
				.withArguments(new StringArgument("player").replaceSuggestions(ALL_CACHED_PLAYER_NAMES_SUGGESTIONS)
					.withPermission(PERMISSION_INFO_OTHER))
				.executesPlayer((moderator, args) -> {
					String playerName = (String) args.get("player");
					UUID playerUuid = StringUtils.getUuidFromInput(playerName);

					PlotManager.getPlotInfo(playerUuid).whenComplete((playerPlotInfo, ex) -> {
						if (ex != null) {
							MMLog.severe("Caught exception trying to list plot access for owner " + playerName + " : " + ex.getMessage());
							moderator.sendMessage(Component.text("An error occurred while trying to list plot access. Please report this: " + ex.getMessage(), NamedTextColor.RED));
							MessagingUtils.sendStackTrace(moderator, ex);
							return;
						}

						moderator.sendMessage(Component.text("Displaying info for player ", NamedTextColor.GOLD).append(Component.text(playerName, NamedTextColor.AQUA)));
						plotInfoRaw(moderator, playerPlotInfo);
					});
				})
			)

			/* /plot send <players> [instance] */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_SEND)
				.withPermission(PERMISSION_SEND)
				.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
				.withOptionalArguments(new IntegerArgument("instance", 1))
				.executes((sender, args) -> {
					Integer instance = args.getUnchecked("instance");
					for (Player player : (Collection<Player>) args.get("players")) {
						try {
							if (instance != null) {
								ScoreboardUtils.setScoreboardValue(player, Constants.Objectives.CURRENT_PLOT, instance);
							}
							PlotManager.sendPlayerToPlot(player);
						} catch (Exception ex) {
							sender.sendMessage(Component.text("Failed to send player '" + player.getName() + "' to plot: " + ex.getMessage(), NamedTextColor.RED));
							player.sendMessage(Component.text("An error occurred while trying to send you to a plot. Please report this: " + ex.getMessage(), NamedTextColor.RED));
							MessagingUtils.sendStackTrace(sender, ex);
						}
					}
				})
			)

			/* /plot gui <players> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_GUI)
				.withPermission(PERMISSION_GUI)
				.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
				.executes((sender, args) -> {
					for (Player player : (Collection<Player>) args.getUnchecked("players")) {
						String playerName = player.getName();
						UUID playerUuid = StringUtils.getUuidFromInput(playerName);

						PlotManager.getPlotInfo(player.getUniqueId()).thenCompose(PlotManager.PlotInfo::populateNamesAndHeads).whenComplete((playerPlotInfo, ex) -> {
							if (ex != null) {
								MMLog.severe("Caught exception trying to list plot access for owner " + player.getName() + " : " + ex.getMessage());
								sender.sendMessage(Component.text("An error occurred while trying to list plot access. Please report this: " + ex.getMessage(), NamedTextColor.RED));
								MessagingUtils.sendStackTrace(sender, ex);
								return;
							}

							new PlotAccessGui(player, playerName, playerUuid, playerPlotInfo, PlotAccessGui.MainGuiMode.TELEPORT_MODE, Component.text("Available Plots")).open();
						});
					}
				})
			)

			/* /plot bordergui <players> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_BORDERGUI)
				.withPermission(PERMISSION_BORDERGUI)
				.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
				.executes((sender, args) -> {
					if (!ServerProperties.getShardName().equals("playerplots") &&
						!ServerProperties.getShardName().startsWith("dev")) {
						throw CommandAPI.failWithString("This command is only available on the playerplots world. Current shard: " + ServerProperties.getShardName());
					}
					for (Player player : (Collection<Player>) args.getUnchecked("players")) {
						int plot = ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.OWN_PLOT).orElse(0);
						int currentplot = ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.CURRENT_PLOT).orElse(0);
						if (plot != currentplot) {
							sender.sendMessage(PlotManager.appendPrefix(Component.text("Only the owner of this plot can change its border.", NamedTextColor.RED)));
							player.sendMessage(PlotManager.appendPrefix(Component.text("Only the owner of this plot can change its border.", NamedTextColor.RED)));
						} else {
							if (player.hasPermission("monumenta.plotborderoverride")) {
								new PlotBorderCustomInventory(player, true).openInventory(player, Plugin.getInstance());
							} else {
								new PlotBorderCustomInventory(player, false).openInventory(player, Plugin.getInstance());
							}
						}
					}
				})
			)

			/* /plot new <players> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_NEW)
				.withPermission(PERMISSION_NEW)
				.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
				.executes((sender, args) -> {
					for (Player player : (Collection<Player>) args.getUnchecked("players")) {
						try {
							int score = ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.OWN_PLOT).orElse(0);
							if (score > 0) {
								sender.sendMessage(PlotManager.appendPrefix(Component.text("You cannot create a new plot for this player because they have a nonzero Plot score.", NamedTextColor.RED)));
								player.sendMessage(PlotManager.appendPrefix(Component.text("A new plot could not be made for you because you have a nonzero Plot score. Please report this as a bug.", NamedTextColor.RED)));
							} else {
								RBoardAPI.add("$Plot", "Plot", 1).whenComplete((newInstance, ex) -> Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
									if (ex != null) {
										sender.sendMessage(Component.text("Failed to get new plot score: " + ex.getMessage(), NamedTextColor.RED));
										player.sendMessage(Component.text("An error occurred while fetching a new plot score. Please report this: " + ex.getMessage(), NamedTextColor.RED));
										MessagingUtils.sendStackTrace(player, ex);
									} else {
										ScoreboardUtils.setScoreboardValue(player, Constants.Objectives.OWN_PLOT, newInstance.intValue());
										ScoreboardUtils.setScoreboardValue(player, Constants.Objectives.CURRENT_PLOT, newInstance.intValue());
										PlotManager.sendPlayerToPlot(player);
									}
								}));
							}
						} catch (Exception ex) {
							sender.sendMessage(Component.text("Failed to create new plot for player '" + player.getName() + "': " + ex.getMessage(), NamedTextColor.RED));
							sender.sendMessage(Component.text("An error occurred while creating a new plot. Please report this: " + ex.getMessage(), NamedTextColor.RED));
							MessagingUtils.sendStackTrace(sender, ex);
						}
					}
				})
			)

			/* /plot reset <players> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_RESET)
				.withPermission(PERMISSION_RESET)
				.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
				.executes((sender, args) -> {
					for (Player player : (Collection<Player>) args.getUnchecked("players")) {
						int score = ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.OWN_PLOT).orElse(0);
						if (score == 0) {
							sender.sendMessage(PlotManager.appendPrefix(Component.text("You cannot reset this player's plot because they have a Plot score of zero.", NamedTextColor.RED)));
							player.sendMessage(PlotManager.appendPrefix(Component.text("Your plot could not be reset because your Plot score is zero. Please report this as a bug.", NamedTextColor.RED)));
						} else {
							MonumentaNetworkRelayIntegration.sendPlayerAuditLogMessage("[Plot Manager] " + player.getName() + " reset their plot. Their Plot score was " + score + ".");
							MonumentaNetworkRelayIntegration.sendAuditLogSevereMessage("[Plot Manager] " + player.getName() + " reset their plot. Their Plot score was " + score + ".");
							ScoreboardUtils.setScoreboardValue(player, Constants.Objectives.OWN_PLOT, 0);
							ScoreboardUtils.setScoreboardValue(player, Constants.Objectives.CURRENT_PLOT, 0);
							PlotManager.getPlotInfo(player.getUniqueId()).whenComplete((info, ex) -> {
								if (ex != null) {
									MMLog.severe("Caught exception trying to list plot access for owner " + player.getName() + " : " + ex.getMessage());
									player.sendMessage(Component.text("An error occurred while trying to list plot access. Please report this: " + ex.getMessage(), NamedTextColor.RED));
									MessagingUtils.sendStackTrace(player, ex);
									return;
								}

								for (UUID otherUUID : info.mOtherAccessToOwnerPlot.keySet()) {
									PlotManager.plotAccessRemove(player, player.getUniqueId(), otherUUID);
								}
							});
						}
					}
				})
			)
			//endregion

			.register();
	}

	private static void plotHelp(CommandSender sender) {
		sender.sendMessage(Component.empty());
		sender.sendMessage(Component.text("/plot help", NamedTextColor.GREEN, TextDecoration.BOLD));
		sender.sendMessage(Component.text("You can use the following commands to manage access to your plot.", NamedTextColor.WHITE));
		sender.sendMessage(Component.empty());
		sender.sendMessage(Component.text("/plot ", NamedTextColor.GREEN).append(Component.text("info", NamedTextColor.AQUA)));
		sender.sendMessage(Component.text("This shows access to your plot and other plots you can access.", NamedTextColor.WHITE));
		sender.sendMessage(Component.empty());
		sender.sendMessage(Component.text("/plot ", NamedTextColor.GREEN).append(Component.text("add playerName optionalDuration", NamedTextColor.AQUA)));
		sender.sendMessage(Component.text("This grants ", NamedTextColor.WHITE).append(Component.text("playerName", NamedTextColor.AQUA)).append(Component.text(" access to your plot.", NamedTextColor.WHITE)));
		sender.sendMessage(Component.text("The player does not need to be online to gain access.", NamedTextColor.WHITE));
		sender.sendMessage(Component.text("Use ", NamedTextColor.WHITE).append(Component.text("optionalDuration", NamedTextColor.AQUA)).append(Component.text(" to specify when their access will expire.", NamedTextColor.WHITE)));
		sender.sendMessage(Component.text("For example: ", NamedTextColor.WHITE).append(Component.text("5d3h", NamedTextColor.AQUA)).append(Component.text(" (for 5 days and 3 hours)", NamedTextColor.WHITE)));
		sender.sendMessage(Component.text("The specified duration cannot exceed 365 days.", NamedTextColor.WHITE));
		sender.sendMessage(Component.empty());
		sender.sendMessage(Component.text("/plot ", NamedTextColor.GREEN).append(Component.text("remove playerName", NamedTextColor.AQUA)));
		sender.sendMessage(Component.text("This revokes ", NamedTextColor.WHITE).append(Component.text("playerName", NamedTextColor.AQUA)).append(Component.text("'s access to your plot.", NamedTextColor.WHITE)));
		sender.sendMessage(Component.text("Note that this will ", NamedTextColor.WHITE).append(Component.text("not", NamedTextColor.WHITE, TextDecoration.ITALIC)).append(Component.text(" teleport them out if they are on your plot or if they logged out while on your plot.", NamedTextColor.WHITE)));
	}

	private static void plotInfoRaw(CommandSender sender, PlotManager.PlotInfo info) {
		sender.sendMessage(Component.text("Your plot number is: ", NamedTextColor.GREEN).append(Component.text("#" + info.mOwnedPlotId, NamedTextColor.GOLD)));
		sender.sendMessage(Component.text("Your currently selected plot is: ", NamedTextColor.GREEN).append(Component.text("#" + info.mCurrentPlotId, NamedTextColor.GOLD)));
		if (info.mOtherAccessToOwnerPlot.isEmpty()) {
			sender.sendMessage(Component.text("There are no players with access to your plot", NamedTextColor.GREEN));
		} else {
			sender.sendMessage(Component.text("These players have access to your plot:", NamedTextColor.GREEN));

			info.mOtherAccessToOwnerPlot.forEach((key, otherAccessToOwnerPlot) -> {
				String name = MonumentaRedisSyncIntegration.cachedUuidToName(key);
				if (name != null) {
					Component msg = Component.text("  " + name, NamedTextColor.AQUA);
					if (otherAccessToOwnerPlot.mExpiration > 0) {
						msg = msg.append(Component.text(" Expires: ", NamedTextColor.GREEN)).append(Component.text(MessagingUtils.getTimeDifferencePretty(otherAccessToOwnerPlot.mExpiration), NamedTextColor.AQUA));
					}
					sender.sendMessage(msg);
				}
			});
		}

		if (info.mOwnerAccessToOtherPlots.isEmpty()) {
			sender.sendMessage(Component.text("You don't have access to any other player's plot", NamedTextColor.GREEN));
		} else {
			sender.sendMessage(Component.text("You have access to these other plots:", NamedTextColor.GREEN));

			info.mOwnerAccessToOtherPlots.forEach((key, ownerAccessToOtherPlots) -> {
				String name = MonumentaRedisSyncIntegration.cachedUuidToName(key);
				if (name != null) {
					Component msg = Component.text("  " + name, NamedTextColor.AQUA).append(Component.text(" (#" + ownerAccessToOtherPlots.mPlotId + ")", NamedTextColor.GOLD));
					if (ownerAccessToOtherPlots.mExpiration > 0) {
						msg = msg.append(Component.text(" Expires: ", NamedTextColor.GREEN)).append(Component.text(MessagingUtils.getTimeDifferencePretty(ownerAccessToOtherPlots.mExpiration), NamedTextColor.AQUA));
					}
					sender.sendMessage(msg);
				}
			});
		}
	}
}

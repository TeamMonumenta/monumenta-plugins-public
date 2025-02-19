package com.playmonumenta.plugins.plots;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.RBoardAPI;
import com.playmonumenta.redissync.RemoteDataAPI;
import com.playmonumenta.worlds.paper.MonumentaWorldManagementAPI;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

public class PlotManager {
	@SuppressWarnings("unchecked")
	public static void registerCommands() {
		IntegerArgument regionArg = new IntegerArgument("regionNum", 0, 3);
		MultiLiteralArgument multiRegionArg = new MultiLiteralArgument("region", "all", "valley", "isles", "ring");

		new CommandAPICommand("plot")
			.withPermission(CommandPermission.NONE)
			/* ACCESS */
			.withSubcommand(new CommandAPICommand("access")
				/* HELP */
				.withSubcommand(new CommandAPICommand("help")
					.executesPlayer((player, args) -> {
						plotAccessHelp(player);
					}))
				/* INFO */
				.withSubcommand(new CommandAPICommand("info")
					.executesPlayer((player, args) -> {
						String name = player.getName();
						UUID uuid = resolveUUID(name);
						getPlotInfo(player.getUniqueId()).thenCompose(PlotInfo::populateNamesAndHeads).whenComplete((info, ex) -> {
							if (ex != null) {
								Plugin.getInstance().getLogger().severe("Caught exception trying to list plot access for owner " + player.getName() + " : " + ex.getMessage());
								player.sendMessage(Component.text("An error occurred while trying to list plot access. Please report this: " + ex.getMessage(), NamedTextColor.RED));
								MessagingUtils.sendStackTrace(player, ex);
							} else {
								new PlotAccessGui(player, name, uuid, info, PlotAccessGui.MainGuiMode.ACCESS_INFO_MODE, Component.text("Plot Access Information")).open();
							}
						});
					}))
				/* INFO OTHER */
				/* This variant requires perms because it lets you get other players */
				.withSubcommand(new CommandAPICommand("info")
					.withArguments(new StringArgument("name")
						.withPermission(CommandPermission.fromString("monumenta.plot.info")))
					.executesPlayer((player, args) -> {
						String name = args.getUnchecked("name");
						UUID uuid = resolveUUID(name);
						getPlotInfo(uuid).thenCompose(PlotInfo::populateNamesAndHeads).whenComplete((info, ex) -> {
							if (ex != null) {
								Plugin.getInstance().getLogger().severe("Caught exception trying to list plot access for owner " + name + " : " + ex.getMessage());
								player.sendMessage(Component.text("An error occurred while trying to list plot access. Please report this: " + ex.getMessage(), NamedTextColor.RED));
								MessagingUtils.sendStackTrace(player, ex);
							} else {
								new PlotAccessGui(player, name, uuid, info, PlotAccessGui.MainGuiMode.ACCESS_INFO_MODE, Component.text("Plot Access Information")).open();
							}
						});
					}))
				/* INFO RAW */
				.withSubcommand(new CommandAPICommand("info_raw")
					.executesPlayer((player, args) -> {
						getPlotInfo(player.getUniqueId()).whenComplete((info, ex) -> {
							if (ex != null) {
								Plugin.getInstance().getLogger().severe("Caught exception trying to list plot access for owner " + player.getName() + " : " + ex.getMessage());
								player.sendMessage(Component.text("An error occurred while trying to list plot access. Please report this: " + ex.getMessage(), NamedTextColor.RED));
								MessagingUtils.sendStackTrace(player, ex);
							} else {
								plotAccessInfoRaw(player, info);
							}
						});
					}))
				/* INFO RAW OTHER */
				/* This variant requires perms because it lets you get other players */
				.withSubcommand(new CommandAPICommand("info_raw")
					.withArguments(new StringArgument("name")
						.withPermission(CommandPermission.fromString("monumenta.plot.info")))
					.executesPlayer((player, args) -> {
						String name = args.getUnchecked("name");
						UUID uuid = resolveUUID(name);
						getPlotInfo(uuid).whenComplete((info, ex) -> {
							if (ex != null) {
								Plugin.getInstance().getLogger().severe("Caught exception trying to list plot access for owner " + name + " : " + ex.getMessage());
								player.sendMessage(Component.text("An error occurred while trying to list plot access. Please report this: " + ex.getMessage(), NamedTextColor.RED));
								MessagingUtils.sendStackTrace(player, ex);
							} else {
								player.sendMessage(Component.text("Displaying info for player ", NamedTextColor.GOLD).append(Component.text(name, NamedTextColor.AQUA)));
								plotAccessInfoRaw(player, info);
							}
						});
					}))
				/* ADD */
				.withSubcommand(new CommandAPICommand("add")
					.withArguments(new StringArgument("name").replaceSuggestions(ArgumentSuggestions.strings(
						(info) -> Bukkit.getOnlinePlayers().stream()
							.filter((player) -> !Objects.equals(player, info.sender()) && !PremiumVanishIntegration.isInvisibleOrSpectator(player))
							.map(Player::getName).toArray(String[]::new))))
					.withOptionalArguments(new StringArgument("duration"))
					.executesPlayer((player, args) -> {
						plotAccessAdd(player, player.getUniqueId(), resolveUUID(args.getUnchecked("name")), args.getUnchecked("duration"));
					}))
				/* MODERATOR ADD */
				.withSubcommand(new CommandAPICommand("add_other")
					.withPermission("monumenta.command.plot.add.others")
					.withArguments(
						new StringArgument("plot owner"),
						new StringArgument("other player")
					)
					.withOptionalArguments(new StringArgument("duration"))
					.executesPlayer((player, args) -> {
						UUID ownerUUID = resolveUUID(args.getUnchecked("plot owner"));
						UUID otherPlayerUUID = resolveUUID(args.getUnchecked("other player"));
						plotAccessAdd(player, ownerUUID, otherPlayerUUID, args.getUnchecked("duration"));
					}))
				/* REMOVE */
				.withSubcommand(new CommandAPICommand("remove")
					.withArguments(new StringArgument("name")) // TODO: Suggestions? Annoying to do
					.executesPlayer((player, args) -> {
						plotAccessRemove(player, args.getUnchecked("name"));
					}))
				/* MODERATOR REMOVE */
				.withSubcommand(new CommandAPICommand("remove_other")
					.withPermission("monumenta.command.plot.remove.others")
					.withArguments(
						new StringArgument("plot owner"),
						new StringArgument("other player"))
					.executesPlayer((player, args) -> {
						UUID ownerUUID = resolveUUID(args.getUnchecked("plot owner"));
						UUID otherPlayerUUID = resolveUUID(args.getUnchecked("other player"));
						plotAccessRemove(player, ownerUUID, otherPlayerUUID);
					}))
			)
			/* SEND */
			.withSubcommand(new CommandAPICommand("send")
				.withPermission(CommandPermission.fromString("monumenta.plot.send"))
				.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
				.withOptionalArguments(new IntegerArgument("instance", 1))
				.executes((sender, args) -> {
					Integer instance = args.getUnchecked("instance");
					for (Player player : (List<Player>) args.get("players")) {
						try {
							if (instance != null) {
								ScoreboardUtils.setScoreboardValue(player, Constants.Objectives.CURRENT_PLOT, instance);
							}
							sendPlayerToPlot(player);
						} catch (Exception ex) {
							sender.sendMessage(Component.text("Failed to send player '" + player.getName() + "' to plot: " + ex.getMessage(), NamedTextColor.RED));
							player.sendMessage(Component.text("An error occurred while trying to send you to a plot. Please report this: " + ex.getMessage(), NamedTextColor.RED));
							MessagingUtils.sendStackTrace(sender, ex);
						}
					}
				}))
			/* GUI */
			.withSubcommand(new CommandAPICommand("gui")
				.withPermission(CommandPermission.fromString("monumenta.plot.gui"))
				.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
				.executes((sender, args) -> {
					for (Player player : (List<Player>) args.get("players")) {
						String name = player.getName();
						UUID uuid = resolveUUID(name);
						getPlotInfo(player.getUniqueId()).thenCompose(PlotInfo::populateNamesAndHeads).whenComplete((info, ex) -> {
							if (ex != null) {
								Plugin.getInstance().getLogger().severe("Caught exception trying to list plot access for owner " + player.getName() + " : " + ex.getMessage());
								sender.sendMessage(Component.text("An error occurred while trying to list plot access. Please report this: " + ex.getMessage(), NamedTextColor.RED));
								MessagingUtils.sendStackTrace(sender, ex);
							} else {
								new PlotAccessGui(player, name, uuid, info, PlotAccessGui.MainGuiMode.TELEPORT_MODE, Component.text("Available Plots")).open();
							}
						});
					}
				}))
			/* BORDER GUI */
			.withSubcommand(new CommandAPICommand("bordergui")
				.withPermission(CommandPermission.fromString("monumenta.plot.bordergui"))
				.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
				.executes((sender, args) -> {
					if (!ServerProperties.getShardName().equals("playerplots") &&
						!ServerProperties.getShardName().startsWith("dev")) {
						throw CommandAPI.failWithString("This command is only available on the playerplots world. Current shard: " + ServerProperties.getShardName());
					}
					for (Player player : (List<Player>) args.get("players")) {
						int plot = ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.OWN_PLOT).orElse(0);
						int currentplot = ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.CURRENT_PLOT).orElse(0);
						if (plot != currentplot) {
							sender.sendMessage(Component.text("Only the owner of this plot can change its border", NamedTextColor.RED));
							player.sendMessage(Component.text("Only the owner of this plot can change its border", NamedTextColor.RED));
						} else {
							if (player.hasPermission("monumenta.plotborderoverride")) {
								new PlotBorderCustomInventory(player, true).openInventory(player, Plugin.getInstance());
							} else {
								new PlotBorderCustomInventory(player, false).openInventory(player, Plugin.getInstance());
							}
						}
					}
				}))
			/* NEW */
			.withSubcommand(new CommandAPICommand("new")
				.withPermission(CommandPermission.fromString("monumenta.plot.new"))
				.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
				.executes((sender, args) -> {
					for (Player player : (List<Player>) args.get("players")) {
						try {
							int score = ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.OWN_PLOT).orElse(0);
							if (score > 0) {
								sender.sendMessage(Component.text("Can't create new plot for player that has nonzero Plot score", NamedTextColor.RED));
								player.sendMessage(Component.text("A new plot couldn't be made for you because you have a nonzero Plot score. Please report this as a bug.", NamedTextColor.RED));
							} else {
								RBoardAPI.add("$Plot", "Plot", 1).whenComplete((newInstance, ex) -> Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
									if (ex != null) {
										sender.sendMessage(Component.text("Failed to get new plot score: " + ex.getMessage(), NamedTextColor.RED));
										player.sendMessage(Component.text("An error occurred while fetching a new plot score. Please report this: " + ex.getMessage(), NamedTextColor.RED));
										MessagingUtils.sendStackTrace(player, ex);
									} else {
										ScoreboardUtils.setScoreboardValue(player, Constants.Objectives.OWN_PLOT, newInstance.intValue());
										ScoreboardUtils.setScoreboardValue(player, Constants.Objectives.CURRENT_PLOT, newInstance.intValue());
										sendPlayerToPlot(player);
									}
								}));
							}
						} catch (Exception ex) {
							sender.sendMessage(Component.text("Failed to create new plot for player '" + player.getName() + "': " + ex.getMessage(), NamedTextColor.RED));
							sender.sendMessage(Component.text("An error occurred while creating a new plot. Please report this: " + ex.getMessage(), NamedTextColor.RED));
							MessagingUtils.sendStackTrace(sender, ex);
						}
					}
				}))
			/* RESET */
			.withSubcommand(new CommandAPICommand("reset")
				.withPermission(CommandPermission.fromString("monumenta.plot.reset"))
				.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
				.executes((sender, args) -> {
					for (Player player : (List<Player>) args.get("players")) {
						int score = ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.OWN_PLOT).orElse(0);
						if (score == 0) {
							sender.sendMessage(Component.text("Can't reset plot for player that has a Plot score of zero", NamedTextColor.RED));
							player.sendMessage(Component.text("Your plot couldn't be reset because your Plot score is zero. Please report this as a bug.", NamedTextColor.RED));
						} else {
							MonumentaNetworkRelayIntegration.sendPlayerAuditLogMessage(player.getName() + " reset their plot (" + score + ")");
							MonumentaNetworkRelayIntegration.sendAuditLogSevereMessage(player.getName() + " reset their plot (" + score + ")");
							ScoreboardUtils.setScoreboardValue(player, Constants.Objectives.OWN_PLOT, 0);
							ScoreboardUtils.setScoreboardValue(player, Constants.Objectives.CURRENT_PLOT, 0);
							getPlotInfo(player.getUniqueId()).whenComplete((info, ex) -> {
								if (ex != null) {
									Plugin.getInstance().getLogger().severe("Caught exception trying to list plot access for owner " + player.getName() + " : " + ex.getMessage());
									player.sendMessage(Component.text("An error occurred while trying to list plot access. Please report this: " + ex.getMessage(), NamedTextColor.RED));
									MessagingUtils.sendStackTrace(player, ex);
								} else {
									for (UUID otherUUID : info.mOtherAccessToOwnerPlot.keySet()) {
										plotAccessRemove(player, player.getUniqueId(), otherUUID);
									}
								}
							});
						}
					}
				}))
			/* REGION */
			.withSubcommand(new CommandAPICommand("region")
				.withPermission(CommandPermission.fromString("monumenta.plot.region"))
				.withArguments(regionArg)
				.executesPlayer((player, args) -> {
					int region = args.getByArgument(regionArg);
					setPlotRegion(player, region);
				}))
			.withSubcommand(new CommandAPICommand("region")
				.withPermission(CommandPermission.fromString("monumenta.plot.region"))
				.withArguments(multiRegionArg)
				.executesPlayer((player, args) -> {
					String regionString = args.getByArgument(multiRegionArg);
					int region = switch (regionString) {
						case "all" -> 0;
						case "valley" -> 1;
						case "isles" -> 2;
						case "ring" -> 3;
						default -> 0;
					};
					setPlotRegion(player, region);
				}))
			.register();
	}

	private static void plotAccessHelp(CommandSender sender) {
		sender.sendMessage(Component.empty());
		sender.sendMessage(Component.text("/plot access help", NamedTextColor.GREEN, TextDecoration.BOLD));
		sender.sendMessage(Component.text("This command is used to manage access to your plot", NamedTextColor.WHITE));
		sender.sendMessage(Component.empty());
		sender.sendMessage(Component.text("/plot access ", NamedTextColor.GREEN).append(Component.text("info", NamedTextColor.AQUA)));
		sender.sendMessage(Component.text("This shows access to your plot and other plots you can access", NamedTextColor.WHITE));
		sender.sendMessage(Component.empty());
		sender.sendMessage(Component.text("/plot access ", NamedTextColor.GREEN).append(Component.text("add playerName optionalDuration", NamedTextColor.AQUA)));
		sender.sendMessage(Component.text("This grants ", NamedTextColor.WHITE).append(Component.text("playerName", NamedTextColor.AQUA)).append(Component.text(" access to your plot", NamedTextColor.WHITE)));
		sender.sendMessage(Component.text("The player does not need to be online to gain access", NamedTextColor.WHITE));
		sender.sendMessage(Component.text("Use ", NamedTextColor.WHITE).append(Component.text("optionalDuration", NamedTextColor.AQUA)).append(Component.text(" to specify when their access will expire", NamedTextColor.WHITE)));
		sender.sendMessage(Component.text("For example: ", NamedTextColor.WHITE).append(Component.text("5d3h", NamedTextColor.AQUA)).append(Component.text(" (for 5 days and 3 hours)", NamedTextColor.WHITE)));
		sender.sendMessage(Component.text("The specified duration cannot exceed 365 days", NamedTextColor.WHITE));
		sender.sendMessage(Component.empty());
		sender.sendMessage(Component.text("/plot access ", NamedTextColor.GREEN).append(Component.text("remove playerName", NamedTextColor.AQUA)));
		sender.sendMessage(Component.text("This revokes ", NamedTextColor.WHITE).append(Component.text("playerName", NamedTextColor.AQUA)).append(Component.text("'s access to your plot", NamedTextColor.WHITE)));
		sender.sendMessage(Component.text("Note that this will ", NamedTextColor.WHITE).append(Component.text("not", NamedTextColor.WHITE, TextDecoration.ITALIC)).append(Component.text(" teleport them out if they are on your plot or if they logged out while on your plot", NamedTextColor.WHITE)));
	}

	private static boolean plotAccessIsExpired(long time) {
		return time >= 0 && Instant.now().getEpochSecond() > time;
	}

	private static void plotAccessInfoRaw(CommandSender sender, PlotInfo info) {
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

	private static void plotAccessAdd(CommandSender sender, UUID ownerUUID, UUID otherUUID, @Nullable String duration) throws WrapperCommandSyntaxException {
		long expiration = -1;
		if (duration != null) {
			duration = duration.toUpperCase(Locale.getDefault());
			if (duration.contains("D")) {
				duration = "P" + (duration.endsWith("D") ? duration : duration.replaceAll("D", "DT"));
			} else {
				duration = "PT" + duration;
			}
			try {
				expiration = Duration.parse(duration).getSeconds();
				if (expiration > 365 * 24 * 60 * 60) {
					throw CommandAPI.failWithString("Duration must be at most 1 year");
				} else if (expiration < 60) {
					throw CommandAPI.failWithString("Duration must be at least 1 minute");
				}

				// Add the current time, so we store the time when it expires, not the delta
				expiration += Instant.now().getEpochSecond();
			} catch (Exception ex) {
				throw CommandAPI.failWithString("Caught exception: " + ex.getMessage());
			}
		}

		if (ownerUUID.equals(otherUUID)) {
			if (sender instanceof Player player && player.getUniqueId().equals(ownerUUID)) {
				throw CommandAPI.failWithString("You cannot add yourself to your own plot");
			} else {
				throw CommandAPI.failWithString("You cannot grant a player access to their own plot");
			}
		}

		Player ownerPlayer = Bukkit.getPlayer(ownerUUID);
		if (ownerPlayer != null) {
			int plot = ScoreboardUtils.getScoreboardValue(ownerPlayer, Constants.Objectives.OWN_PLOT).orElse(0);
			plotAccessAdd(sender, ownerUUID, otherUUID, plot, expiration);
		} else {
			long finalExpiration = expiration;
			MonumentaRedisSyncAPI.getPlayerScores(ownerUUID).thenAccept(scores -> {
				Integer plot = scores.get(Constants.Objectives.OWN_PLOT);
				plotAccessAdd(sender, ownerUUID, otherUUID, plot == null ? 0 : plot, finalExpiration);
			});
		}
	}

	private static void plotAccessAdd(CommandSender sender, UUID ownerUUID, UUID otherUUID, int plot, long expiration) {
		if (plot <= 0) {
			if (sender instanceof Player player && player.getUniqueId().equals(ownerUUID)) {
				sender.sendMessage(Component.text("You currently don't have a plot to add someone to", NamedTextColor.RED));
			} else {
				sender.sendMessage(Component.text(MonumentaRedisSyncIntegration.cachedUuidToName(ownerUUID) + " does not have a plot", NamedTextColor.RED));
			}
			return;
		}

		CompletableFuture<Boolean> futureOwn = RemoteDataAPI.set(ownerUUID, "myplotaccess|" + otherUUID, Long.toString(expiration));
		CompletableFuture<Boolean> futureOther = RemoteDataAPI.set(otherUUID, "otherplotaccess|" + ownerUUID, plot + "," + expiration);
		futureOwn.thenAccept(successOwn -> futureOther.thenAccept(successOther -> {

			String ownerName = MonumentaRedisSyncIntegration.cachedUuidToName(ownerUUID);
			String otherName = MonumentaRedisSyncIntegration.cachedUuidToName(otherUUID);
			String plotName = sender instanceof Player player && player.getUniqueId().equals(ownerUUID) ? "your plot" : "the plot of " + ownerName;

			if (otherName == null) {
				return;
			}

			if (successOwn) {
				sender.sendMessage(Component.text("Successfully granted ", NamedTextColor.GREEN).append(Component.text(otherName, NamedTextColor.AQUA)).append(Component.text(" access to " + plotName, NamedTextColor.GREEN)));
			} else {
				sender.sendMessage(Component.text("Successfully updated the duration of access of ", NamedTextColor.GREEN).append(Component.text(otherName, NamedTextColor.AQUA)).append(Component.text(" to " + plotName, NamedTextColor.GREEN)));
			}

			Player ownerPlayer = Bukkit.getPlayer(ownerUUID);
			if (ownerPlayer != null && ownerPlayer != sender) {
				ownerPlayer.sendMessage(Component.text(otherName, NamedTextColor.AQUA).append(Component.text(" has been granted access to your plot by a moderator", NamedTextColor.GREEN)));
			}
			if (successOther && ownerName != null) {
				Player addedPlayer = Bukkit.getPlayer(otherUUID);
				if (addedPlayer != null) {
					addedPlayer.sendMessage(Component.text("You now have access to ", NamedTextColor.GREEN).append(Component.text(ownerName, NamedTextColor.AQUA)).append(Component.text("'s plot", NamedTextColor.GREEN)));
				}
			}
		}));
	}

	/* TODO: There needs to be some security mechanism that verifies players still have access to a plot if they last visited it but it expired */
	/* Maybe when player joins, fetch their access and see if it's currently expired? And boot them to their own plot if so?  */

	static void plotAccessRemove(Player owner, String removedName) throws WrapperCommandSyntaxException {
		UUID removedUUID = resolveUUID(removedName);

		plotAccessRemove(owner, owner.getUniqueId(), removedUUID);
	}

	static void plotAccessRemove(@Nullable CommandSender sender, UUID ownerUUID, UUID otherUUID) {
		CompletableFuture<Boolean> future1 = RemoteDataAPI.del(ownerUUID, "myplotaccess|" + otherUUID);
		CompletableFuture<Boolean> future2 = RemoteDataAPI.del(otherUUID, "otherplotaccess|" + ownerUUID);
		if (sender != null) {
			future1.thenAccept(success1 -> future2.thenAccept(success2 -> {
				String otherName = MonumentaRedisSyncIntegration.cachedUuidToName(otherUUID);
				if (otherName == null) {
					otherName = otherUUID.toString();
				}
				String ownerName = MonumentaRedisSyncIntegration.cachedUuidToName(ownerUUID);
				if (ownerName == null) {
					ownerName = ownerUUID.toString();
				}
				String plotName = sender instanceof Player player && player.getUniqueId().equals(ownerUUID) ? "your plot" : "the plot of " + ownerName;
				if (success1 && success2) {
					sender.sendMessage(Component.text(otherName + " can no longer access " + plotName, NamedTextColor.GREEN));
				} else {
					sender.sendMessage(Component.text(otherName + " did not have access to " + plotName, NamedTextColor.RED));
				}
			}));
		}
	}

	public static void sendPlayerToPlot(Player player) {
		try {
			if (ServerProperties.getShardName().equals("playerplots")) {
				MonumentaWorldManagementAPI.sortWorld(player);
			} else {
				MonumentaRedisSyncAPI.sendPlayer(player, "playerplots");
			}
		} catch (Exception ex) {
			player.sendMessage(Component.text("An error occurred while sending you to playerplots. Please report this: " + ex.getMessage(), NamedTextColor.RED));
			MessagingUtils.sendStackTrace(player, ex);
		}
	}

	public static class PlotInfo {
		public static class OtherAccessToOwnerPlotRecord {
			final UUID mUUID;
			final long mExpiration;
			@Nullable PlayerProfile mProfile = null;
			@Nullable ItemStack mHead = null;
			@Nullable String mName = null;

			protected OtherAccessToOwnerPlotRecord(UUID uuid, long expiration) {
				mUUID = uuid;
				mExpiration = expiration;
			}
		}

		public static class OwnerAccessToOtherPlotsRecord {
			final int mPlotId;
			final long mExpiration;
			@Nullable PlayerProfile mProfile = null;
			@Nullable ItemStack mHead = null;
			@Nullable String mName = null;

			protected OwnerAccessToOtherPlotsRecord(int plotId, long expiration) {
				mPlotId = plotId;
				mExpiration = expiration;
			}
		}

		final UUID mUUID;

		// Plot ID this player owns
		final int mOwnedPlotId;
		// Plot ID this player currently has selected
		final int mCurrentPlotId;

		// Access other players have to owner plot, uuid | expiration
		final Map<UUID, OtherAccessToOwnerPlotRecord> mOtherAccessToOwnerPlot;
		// Access owner has to other plots, uuid | expiration
		final Map<UUID, OwnerAccessToOtherPlotsRecord> mOwnerAccessToOtherPlots;

		protected PlotInfo(UUID uuid, int ownedPlotId, int currentPlotId, Map<UUID, OtherAccessToOwnerPlotRecord> otherAccessToOwnerPlot, Map<UUID, OwnerAccessToOtherPlotsRecord> ownerAccessToOtherPlots) {
			mUUID = uuid;
			mOwnedPlotId = ownedPlotId;
			mCurrentPlotId = currentPlotId;
			mOtherAccessToOwnerPlot = otherAccessToOwnerPlot;
			mOwnerAccessToOtherPlots = ownerAccessToOtherPlots;
		}

		/* Call this to fetch the player's name and head */
		public CompletableFuture<PlotInfo> populateNamesAndHeads() {
			CompletableFuture<PlotInfo> future = new CompletableFuture<>();

			/* Populate all the names and (empty) profiles on the main thread */
			for (Map.Entry<UUID, OtherAccessToOwnerPlotRecord> entry : mOtherAccessToOwnerPlot.entrySet()) {
				UUID uuid = entry.getKey();
				OtherAccessToOwnerPlotRecord rec = entry.getValue();
				rec.mName = MonumentaRedisSyncIntegration.cachedUuidToName(uuid);
				rec.mProfile = Bukkit.getServer().createProfile(uuid, rec.mName);
			}

			for (Map.Entry<UUID, OwnerAccessToOtherPlotsRecord> entry : mOwnerAccessToOtherPlots.entrySet()) {
				UUID uuid = entry.getKey();
				OwnerAccessToOtherPlotsRecord rec = entry.getValue();
				rec.mName = MonumentaRedisSyncIntegration.cachedUuidToName(uuid);
				rec.mProfile = Bukkit.getServer().createProfile(uuid, rec.mName);
			}

			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				/* Complete all the profiles async */
				for (Map.Entry<UUID, OtherAccessToOwnerPlotRecord> entry : mOtherAccessToOwnerPlot.entrySet()) {
					PlayerProfile profile = entry.getValue().mProfile;

					if (profile != null) {
						profile.complete();
					}
				}

				for (Map.Entry<UUID, OwnerAccessToOtherPlotsRecord> entry : mOwnerAccessToOtherPlots.entrySet()) {
					PlayerProfile profile = entry.getValue().mProfile;

					if (profile != null) {
						profile.complete();
					}
				}

				Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
					/* Switch back to the main thread to finish assembling all the heads */
					for (Map.Entry<UUID, OtherAccessToOwnerPlotRecord> entry : mOtherAccessToOwnerPlot.entrySet()) {
						OtherAccessToOwnerPlotRecord rec = entry.getValue();

						ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
						SkullMeta meta = (SkullMeta) head.getItemMeta();
						meta.setPlayerProfile(rec.mProfile);
						meta.displayName(Component.text(rec.mName, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
						List<Component> lore = new ArrayList<>();
						lore.add(Component.text("Access expires in:", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
						String timeLeft = (rec.mExpiration == -1) ? "Unlimited" : MessagingUtils.getTimeDifferencePretty(rec.mExpiration);
						lore.add(Component.text(timeLeft, NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
						meta.lore(lore);
						meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
						head.setItemMeta(meta);

						rec.mHead = head;
					}

					for (Map.Entry<UUID, OwnerAccessToOtherPlotsRecord> entry : mOwnerAccessToOtherPlots.entrySet()) {
						OwnerAccessToOtherPlotsRecord rec = entry.getValue();

						ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
						SkullMeta meta = (SkullMeta) head.getItemMeta();
						meta.setPlayerProfile(rec.mProfile);
						meta.displayName(Component.text(rec.mName + " (#" + rec.mPlotId + ")", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
						List<Component> lore = new ArrayList<>();
						lore.add(Component.text("Access expires in:", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
						String timeLeft = (rec.mExpiration == -1) ? "Unlimited" : MessagingUtils.getTimeDifferencePretty(rec.mExpiration);
						lore.add(Component.text(timeLeft, NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
						meta.lore(lore);
						meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
						head.setItemMeta(meta);

						rec.mHead = head;
					}

					/* Complete the future on the main thread */
					future.complete(this);
				});
			});

			return future;
		}
	}

	/* Should be run on main thread, does work async and completes on main thread */
	static CompletableFuture<PlotInfo> getPlotInfo(UUID ownerUUID) {
		CompletableFuture<PlotInfo> future = new CompletableFuture<>();

		final int plot;
		final int currentPlot;
		final CompletableFuture<Map<String, Integer>> scoresFuture;

		Player owner = Bukkit.getPlayer(ownerUUID);
		if (owner != null) {
			plot = ScoreboardUtils.getScoreboardValue(owner, Constants.Objectives.OWN_PLOT).orElse(0);
			currentPlot = ScoreboardUtils.getScoreboardValue(owner, Constants.Objectives.CURRENT_PLOT).orElse(0);
			scoresFuture = null;
		} else {
			// These two values will be overwritten by the scores future
			plot = 0;
			currentPlot = 0;
			scoresFuture = MonumentaRedisSyncAPI.getPlayerScores(ownerUUID);
		}

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				Map<String, String> allRemoteData = RemoteDataAPI.getAll(ownerUUID).get();

				/* If the player wasn't online, fetching scores already started, need to complete that and get the specific interesting values */
				final int finalPlot;
				final int finalCurrentPlot;
				if (scoresFuture != null) {
					Map<String, Integer> scores = scoresFuture.get();
					finalPlot = scores.getOrDefault(Constants.Objectives.OWN_PLOT, 0);
					finalCurrentPlot = scores.getOrDefault(Constants.Objectives.CURRENT_PLOT, 0);
				} else {
					finalPlot = plot;
					finalCurrentPlot = currentPlot;
				}

				Map<UUID, PlotInfo.OtherAccessToOwnerPlotRecord> otherAccessToOwnerPlot = new HashMap<>();
				Map<UUID, PlotInfo.OwnerAccessToOtherPlotsRecord> ownerAccessToOtherPlots = new HashMap<>();

				/* Filter the entries into other access to plot, self access to other plots, and remove expired entries */
				allRemoteData.forEach((key, value) -> {
					if (key.startsWith("myplotaccess|") || key.startsWith("otherplotaccess|")) {
						UUID otherUUID;
						long expiration;
						if (key.startsWith("myplotaccess|")) {
							otherUUID = UUID.fromString(key.substring("myplotaccess|".length()));
							expiration = Long.parseLong(value);
							if (plotAccessIsExpired(expiration)) {
								plotAccessRemove(null, ownerUUID, otherUUID);
							} else {
								otherAccessToOwnerPlot.put(otherUUID, new PlotInfo.OtherAccessToOwnerPlotRecord(otherUUID, expiration));
							}
						} else {
							otherUUID = UUID.fromString(key.substring("otherplotaccess|".length()));
							String[] split = value.split(",");
							int plotId = Integer.parseInt(split[0]);
							expiration = Long.parseLong(split[1]);
							if (plotAccessIsExpired(expiration)) {
								plotAccessRemove(null, otherUUID, ownerUUID);
							} else {
								ownerAccessToOtherPlots.put(otherUUID, new PlotInfo.OwnerAccessToOtherPlotsRecord(plotId, expiration));
							}
						}
					}
				});

				PlotInfo info = new PlotInfo(ownerUUID, finalPlot, finalCurrentPlot, otherAccessToOwnerPlot, ownerAccessToOtherPlots);

				/* Complete future on main thread for easy use of .whenCompleted() */
				Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> future.complete(info));
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> future.completeExceptionally(ex));
			}
		});

		return future;
	}

	private static UUID resolveUUID(String name) throws WrapperCommandSyntaxException {
		try {
			return UUID.fromString(name);
		} catch (IllegalArgumentException e) {
			UUID uuid = MonumentaRedisSyncIntegration.cachedNameToUuid(name);
			if (uuid == null) {
				throw CommandAPI.failWithString("The player name '" + name + "' couldn't be found. Please check your spelling and ensure proper capitalization.");
			}
			return uuid;
		}
	}

	public static int getPlotRegion(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "PlotRegion").orElse(0);
	}

	public static void setPlotRegion(Player player, int region) throws WrapperCommandSyntaxException {
		String[] regionNames = {"all", "King's Valley", "Celsian Isles", "Architect's Ring"};
		region = Math.max(0, Math.min(region, regionNames.length - 1));
		String regionName = regionNames[region];
		int oldRegion = getPlotRegion(player);
		if (oldRegion == region) {
			player.sendMessage(Component.text("Your plot region is already set as " + regionName, NamedTextColor.GOLD));
			return;
		}
		if (region == 3 && !PlayerUtils.hasUnlockedRing(player)) {
			throw CommandAPI.failWithString("You cannot set your plot region to Architect's Ring before discovering it");
		}
		if (region == 2 && !PlayerUtils.hasUnlockedIsles(player)) {
			throw CommandAPI.failWithString("You cannot set your plot region to Celsian Isles before discovering it");
		}
		ScoreboardUtils.setScoreboardValue(player, "PlotRegion", region);
		AbilityUtils.refreshClass(player);
		Plugin.getInstance().mItemStatManager.updateStats(player);
		player.sendMessage(Component.text("Your plot region has been set to " + regionName, NamedTextColor.GOLD));
	}
}

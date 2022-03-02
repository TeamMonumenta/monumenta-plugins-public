package com.playmonumenta.plugins.plots;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.worlds.paper.MonumentaWorldManagementAPI;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlotManager {
	public PlotManager() {
		registerCommands();
	}

	@SuppressWarnings("unchecked")
	private static void registerCommands() {
		new CommandAPICommand("plot")
			.withPermission(CommandPermission.NONE)
			/********************* ACCESS *********************/
			.withSubcommand(new CommandAPICommand("access")
				/***** HELP *****/
				.withSubcommand(new CommandAPICommand("help")
					.executesPlayer((player, args) -> {
						plotAccessHelp(player);
					}))
				/***** INFO *****/
				.withSubcommand(new CommandAPICommand("info")
					.executesPlayer((player, args) -> {
						getPlotInfo(player.getUniqueId()).whenComplete((info, ex) -> {
							if (ex != null) {
								Plugin.getInstance().getLogger().severe("Caught exception trying to list plot access for owner " + player.getName() + " : " + ex.getMessage());
								player.sendMessage(ChatColor.RED + "Got error trying to list plot access, please report this: " + ex.getMessage());
								ex.printStackTrace();
							} else {
								plotAccessInfo(player, info);
							}
						});
					}))
				/* This variant requires perms because it lets you get other players */
				.withSubcommand(new CommandAPICommand("info")
					.withPermission(CommandPermission.fromString("monumenta.plot.info"))
					.withArguments(new StringArgument("name"))
					.executes((sender, args) -> {
						String name = (String) args[0];
						UUID uuid = MonumentaRedisSyncIntegration.cachedNameToUuid(name);
						if (uuid == null) {
							CommandAPI.fail("Can't find player '" + name + "' - perhaps incorrect capitalization or spelled wrong?");
						}
						getPlotInfo(uuid).whenComplete((info, ex) -> {
							if (ex != null) {
								Plugin.getInstance().getLogger().severe("Caught exception trying to list plot access for owner " + name + " : " + ex.getMessage());
								sender.sendMessage(ChatColor.RED + "Got error trying to list plot access, please report this: " + ex.getMessage());
								ex.printStackTrace();
							} else {
								sender.sendMessage(ChatColor.GOLD + "Displaying info for player " + ChatColor.AQUA + name);
								plotAccessInfo(sender, info);
							}
						});
					}))
				/***** ADD *****/
				.withSubcommand(new CommandAPICommand("add")
					.withArguments(new StringArgument("name").replaceSuggestions((info) -> {
						return Bukkit.getOnlinePlayers().stream().filter((player) -> !Objects.equals(player, info.sender())).map((player) -> player.getName()).toArray(String[]::new);
					}))
					.executesPlayer((player, args) -> {
						plotAccessAdd(player, (String)args[0], null);
					}))
				.withSubcommand(new CommandAPICommand("add")
					.withArguments(new StringArgument("name").replaceSuggestions((info) -> {
						return Bukkit.getOnlinePlayers().stream().filter((player) -> !Objects.equals(player, info.sender())).map((player) -> player.getName()).toArray(String[]::new);
					}))
					.withArguments(new StringArgument("duration"))
					.executesPlayer((player, args) -> {
						plotAccessAdd(player, (String)args[0], (String)args[1]);
					}))
				/***** REMOVE *****/
				.withSubcommand(new CommandAPICommand("remove")
					.withArguments(new StringArgument("name")) // TODO: Suggestions? Annoying to do
					.executesPlayer((player, args) -> {
						plotAccessRemove(player, (String)args[0]);
					}))
			)
			/********************* SEND *********************/
			.withSubcommand(new CommandAPICommand("send")
				.withPermission(CommandPermission.fromString("monumenta.plot.send"))
				.withArguments(new EntitySelectorArgument("players", EntitySelector.MANY_PLAYERS))
				.executes((sender, args) -> {
					for (Player player : (List<Player>)args[0]) {
						try {
							sendPlayerToPlot(player);
						} catch (Exception ex) {
							sender.sendMessage(ChatColor.RED + "Failed to send player to plot '" + player.getName() + "': " + ex.getMessage());
							sender.sendMessage(ChatColor.RED + "Failed to send you to plot, please report this: " + ex.getMessage());
							ex.printStackTrace();
						}
					}
				}))
			.withSubcommand(new CommandAPICommand("send")
				.withPermission(CommandPermission.fromString("monumenta.plot.send"))
				.withArguments(new EntitySelectorArgument("players", EntitySelector.MANY_PLAYERS))
				.withArguments(new IntegerArgument("instance", 1))
				.executes((sender, args) -> {
					for (Player player : (List<Player>)args[0]) {
						try {
							ScoreboardUtils.setScoreboardValue(player, "CurrentPlot", (Integer)args[1]);
							sendPlayerToPlot(player);
						} catch (Exception ex) {
							sender.sendMessage(ChatColor.RED + "Failed to send player to plot '" + player.getName() + "': " + ex.getMessage());
							sender.sendMessage(ChatColor.RED + "Failed to send you to plot, please report this: " + ex.getMessage());
							ex.printStackTrace();
						}
					}
				}))
			/********************* GUI *********************/
			.withSubcommand(new CommandAPICommand("gui")
				.withPermission(CommandPermission.fromString("monumenta.plot.gui"))
				.withArguments(new EntitySelectorArgument("players", EntitySelector.MANY_PLAYERS))
				.executes((sender, args) -> {
					for (Player player : (List<Player>)args[0]) {
						getPlotInfo(player.getUniqueId()).thenCompose((info) -> info.populateNamesAndHeads()).whenComplete((info, ex) -> {
							if (ex != null) {
								Plugin.getInstance().getLogger().severe("Caught exception trying to list plot access for owner " + player.getName() + " : " + ex.getMessage());
								sender.sendMessage(ChatColor.RED + "Got error trying to list plot access, please report this: " + ex.getMessage());
								ex.printStackTrace();
							} else {
								new PlotAccessCustomInventory(player, info).openInventory(player, Plugin.getInstance());
							}
						});
					}
				}))
			.withSubcommand(new CommandAPICommand("bordergui")
				.withPermission(CommandPermission.fromString("monumenta.plot.bordergui"))
				.withArguments(new EntitySelectorArgument("players", EntitySelector.MANY_PLAYERS))
				.executes((sender, args) -> {
					if (!ServerProperties.getShardName().equals("playerplots")) {
						CommandAPI.fail("This command is only available on the playerplots world");
						return;
					}
					for (Player player : (List<Player>)args[0]) {
						int plot = ScoreboardUtils.getScoreboardValue(player, "Plot").orElse(0);
						int currentplot = ScoreboardUtils.getScoreboardValue(player, "CurrentPlot").orElse(0);
						if (plot != currentplot) {
							sender.sendMessage(ChatColor.RED + "Only the owner of this plot can change its border");
							player.sendMessage(ChatColor.RED + "Only the owner of this plot can change its border");
						} else {
							if (player.hasPermission("monumenta.plotborderoverride")) {
								new PlotBorderCustomInventory(player, true).openInventory(player, Plugin.getInstance());
							} else {
								new PlotBorderCustomInventory(player, false).openInventory(player, Plugin.getInstance());
							}
						}
					}
				}))
			/********************* NEW *********************/
			.withSubcommand(new CommandAPICommand("new")
				.withPermission(CommandPermission.fromString("monumenta.plot.new"))
				.withArguments(new EntitySelectorArgument("players", EntitySelector.MANY_PLAYERS))
				.executes((sender, args) -> {
					for (Player player : (List<Player>)args[0]) {
						try {
							int score = ScoreboardUtils.getScoreboardValue(player, "Plot").orElse(0);
							if (score > 0) {
								sender.sendMessage(ChatColor.RED + "Can't create new plot for player that has nonzero Plot score");
								player.sendMessage(ChatColor.RED + "Can't create new plot for you because you have a nonzero Plot score. This is a bug, please report it.");
							} else {
								MonumentaRedisSyncAPI.rboardAdd("$Plot", "Plot", 1).whenComplete((newInstance, ex) -> {
									Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
										if (ex != null) {
											sender.sendMessage(ChatColor.RED + "Failed to get new plot score: " + ex.getMessage());
											player.sendMessage(ChatColor.RED + "Failed to get new plot score, please report this: " + ex.getMessage());
											ex.printStackTrace();
										} else {
											ScoreboardUtils.setScoreboardValue(player, "Plot", newInstance.intValue());
											ScoreboardUtils.setScoreboardValue(player, "CurrentPlot", newInstance.intValue());
											sendPlayerToPlot(player);
										}
									});
								});
							}
						} catch (Exception ex) {
							sender.sendMessage(ChatColor.RED + "Failed to create new plot for player '" + player.getName() + "': " + ex.getMessage());
							sender.sendMessage(ChatColor.RED + "Failed to create new plot, please report this: " + ex.getMessage());
							ex.printStackTrace();
						}
					}
				}))
			/********************* RESET *********************/
			.withSubcommand(new CommandAPICommand("reset")
				.withPermission(CommandPermission.fromString("monumenta.plot.reset"))
				.withArguments(new EntitySelectorArgument("players", EntitySelector.MANY_PLAYERS))
				.executes((sender, args) -> {
					for (Player player : (List<Player>)args[0]) {
						int score = ScoreboardUtils.getScoreboardValue(player, "Plot").orElse(0);
						if (score == 0) {
							sender.sendMessage(ChatColor.RED + "Can't reset plot for player that has a Plot score of zero");
							player.sendMessage(ChatColor.RED + "Can't reset your plot because your Plot score is zero. This is a bug, please report it.");
						} else {
							ScoreboardUtils.setScoreboardValue(player, "Plot", 0);
							ScoreboardUtils.setScoreboardValue(player, "CurrentPlot", 0);
							getPlotInfo(player.getUniqueId()).whenComplete((info, ex) -> {
								if (ex != null) {
									Plugin.getInstance().getLogger().severe("Caught exception trying to list plot access for owner " + player.getName() + " : " + ex.getMessage());
									player.sendMessage(ChatColor.RED + "Got error trying to list plot access, please report this: " + ex.getMessage());
									ex.printStackTrace();
								} else {
									for (UUID otherUUID : info.mOtherAccessToOwnerPlot.keySet()) {
										plotAccessRemove(player.getUniqueId(), otherUUID);
									}
								}
							});
						}
					}
				}))
			.register();
	}

	private static void plotAccessHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.GREEN + "");
		sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "/plot access help");
		sender.sendMessage(ChatColor.GREEN + "This command is used to give other people access to your plot");
		sender.sendMessage(ChatColor.GREEN + "/plot access " + ChatColor.AQUA + "list");
		sender.sendMessage(ChatColor.GREEN + "  Lists access to your plot and other plots you can access");
		sender.sendMessage(ChatColor.GREEN + "/plot access " + ChatColor.AQUA + "add playerName optionalDuration");
		sender.sendMessage(ChatColor.GREEN + "  Lets " + ChatColor.AQUA + "playerName" + ChatColor.GREEN + " access your plot");
		sender.sendMessage(ChatColor.GREEN + "  Player does not need to be online");
		sender.sendMessage(ChatColor.GREEN + "  Use " + ChatColor.AQUA + "optionalDuration" + ChatColor.GREEN + " to indicate when access expires");
		sender.sendMessage(ChatColor.GREEN + "  For example 5d3h for 5 days 3 hours; max 365 days");
		sender.sendMessage(ChatColor.GREEN + "/plot access " + ChatColor.AQUA + "remove playerName");
		sender.sendMessage(ChatColor.GREEN + "  Removes " + ChatColor.AQUA + "playerName" + ChatColor.GREEN + " from access your plot");
		sender.sendMessage(ChatColor.GREEN + "  Will *not* teleport them out if they are already there!");
		sender.sendMessage(ChatColor.GREEN + "  (or if they logged out there)");
	}

	private static boolean plotAccessIsExpired(long time) {
		return time >= 0 && java.time.Instant.now().getEpochSecond() > time;
	}

	private static void plotAccessInfo(CommandSender sender, PlotInfo info) {
		sender.sendMessage(ChatColor.GREEN + "Your plot number is: " + ChatColor.GOLD + "#" + info.mOwnedPlotId);
		sender.sendMessage(ChatColor.GREEN + "Your currently selected plot is: " + ChatColor.GOLD + "#" + info.mCurrentPlotId);
		if (info.mOtherAccessToOwnerPlot.size() == 0) {
			sender.sendMessage(ChatColor.GREEN + "There are no players with access to your plot");
		} else {
			sender.sendMessage(ChatColor.GREEN + "These players have access to your plot:");

			info.mOtherAccessToOwnerPlot.forEach((key, expiration) -> {
				if (expiration <= 0) {
					sender.sendMessage("  " + ChatColor.AQUA + MonumentaRedisSyncIntegration.cachedUuidToName(key));
				} else {
					sender.sendMessage("  " + ChatColor.AQUA + MonumentaRedisSyncIntegration.cachedUuidToName(key) + ChatColor.GREEN + " Expires: " + ChatColor.AQUA + MessagingUtils.getTimeDifferencePretty(expiration));
				}
			});
		}

		if (info.mOwnerAccessToOtherPlots.size() == 0) {
			sender.sendMessage(ChatColor.GREEN + "You don't have access to any other player's plot");
		} else {
			sender.sendMessage(ChatColor.GREEN + "You have access to these other plots:");

			info.mOwnerAccessToOtherPlots.forEach((key, other) -> {
				if (other.mExpiration <= 0) {
					sender.sendMessage("  " + ChatColor.AQUA + MonumentaRedisSyncIntegration.cachedUuidToName(key) + ChatColor.GOLD + " (#" + other.mPlotId + ")");
				} else {
					sender.sendMessage("  " + ChatColor.AQUA + MonumentaRedisSyncIntegration.cachedUuidToName(key) + ChatColor.GOLD + " (#" + other.mPlotId + ")" + ChatColor.GREEN + " Expires: " + ChatColor.AQUA + MessagingUtils.getTimeDifferencePretty(other.mExpiration));
				}
			});
		}
	}

	private static void plotAccessAdd(Player owner, String addedName, @Nullable String duration) throws WrapperCommandSyntaxException {
		UUID addedUUID = MonumentaRedisSyncIntegration.cachedNameToUuid(addedName);
		if (addedUUID == null) {
			CommandAPI.fail("Can't find player '" + addedName + "' - perhaps incorrect capitalization or spelled wrong?");
		}

		long expiration = -1;
		if (duration != null) {
			duration = duration.toUpperCase();
			if (duration.contains("D")) {
				duration = "P" + (duration.endsWith("D") ? duration : duration.replaceAll("D", "DT"));
			} else {
				duration = "PT" + duration;
			}
			try {
				expiration = Duration.parse(duration).getSeconds();
				if (expiration > 365 * 24 * 60 * 60) {
					CommandAPI.fail("Duration must be at most 1 year");
				} else if (expiration < 60) {
					CommandAPI.fail("Duration must be at least 1 minute");
				}

				// Add the current time, so we store the time when it expires, not the delta
				expiration += java.time.Instant.now().getEpochSecond();
			} catch (Exception ex) {
				CommandAPI.fail("Caught exception: " + ex.getMessage());
			}
		}

		if (addedUUID.equals(owner.getUniqueId())) {
			CommandAPI.fail("You can not add yourself to your own plot");
		}

		UUID ownerUUID = owner.getUniqueId();
		int score = ScoreboardUtils.getScoreboardValue(owner, "Plot").orElse(0);
		if (score <= 0) {
			CommandAPI.fail("You don't currently have a plot to add someone to");
		} else {
			try {
				MonumentaRedisSyncAPI.setRemoteData(ownerUUID, "myplotaccess|" + addedUUID, Long.toString(expiration));
				MonumentaRedisSyncAPI.setRemoteData(addedUUID, "otherplotaccess|" + ownerUUID, Integer.toString(score) + "," + Long.toString(expiration));
			} catch (Exception ex) {
				String msg = "Caught exception while adding plot access. owner=" + ownerUUID + " other=" + addedUUID;
				Plugin.getInstance().getLogger().severe(msg);
				ex.printStackTrace();
				CommandAPI.fail(msg);
			}
		}

		owner.sendMessage(ChatColor.GREEN + "Successfully added " + ChatColor.AQUA + addedName + ChatColor.GREEN + " to access your plot");
		Player added = Bukkit.getPlayer(addedUUID);
		if (added != null) {
			added.sendMessage(ChatColor.GREEN + "You now have access to " + ChatColor.AQUA + owner.getName() + ChatColor.GREEN + "'s plot");
		}
	}
	/* TODO: There needs to be some security mechanism that verifies players still have access to a plot if they last visited it but it expired */
	/* Maybe when player joins, fetch their access and see if it's currently expired? And boot them to their own plot if so?  */

	private static void plotAccessRemove(Player owner, String removedName) throws WrapperCommandSyntaxException {
		UUID removedUUID = MonumentaRedisSyncIntegration.cachedNameToUuid(removedName);
		if (removedUUID == null) {
			CommandAPI.fail("Can't find player '" + removedName + "' - perhaps incorrect capitalization or spelled wrong?");
		}

		plotAccessRemove(owner.getUniqueId(), removedUUID);

		owner.sendMessage(ChatColor.GREEN + "Player '" + removedName + "' no longer has access to your plot");
	}

	private static void plotAccessRemove(UUID ownerUUID, UUID otherUUID) {
		try {
			MonumentaRedisSyncAPI.delRemoteData(ownerUUID, "myplotaccess|" + otherUUID);
			MonumentaRedisSyncAPI.delRemoteData(otherUUID, "otherplotaccess|" + ownerUUID);
		} catch (Exception ex) {
			Plugin.getInstance().getLogger().severe("Caught exception while removing plot access. owner=" + ownerUUID + " other=" + otherUUID);
			ex.printStackTrace();
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
			player.sendMessage(ChatColor.RED + "Failed to send you to playerplots, please report this: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	public static class PlotInfo {
		public static class OtherAccessRecord {
			final int mPlotId;
			final long mExpiration;
			@Nullable PlayerProfile mProfile = null;
			@Nullable ItemStack mHead = null;
			@Nullable String mName = null;

			protected OtherAccessRecord(int plotId, long expiration) {
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
		final Map<UUID, Long> mOtherAccessToOwnerPlot;
		// Access owner has to other plots, uuid | expiration
		final Map<UUID, OtherAccessRecord> mOwnerAccessToOtherPlots;

		protected PlotInfo(UUID uuid, int ownedPlotId, int currentPlotId, Map<UUID, Long> otherAccessToOwnerPlot, Map<UUID, OtherAccessRecord> ownerAccessToOtherPlots) {
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
			for (Map.Entry<UUID, OtherAccessRecord> entry : mOwnerAccessToOtherPlots.entrySet()) {
				UUID uuid = entry.getKey();
				OtherAccessRecord rec = entry.getValue();
				rec.mName = MonumentaRedisSyncIntegration.cachedUuidToName(uuid);
				rec.mProfile = Bukkit.getServer().createProfile(uuid, rec.mName);
			}

			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				/* Complete all of the profiles async */
				for (Map.Entry<UUID, OtherAccessRecord> entry : mOwnerAccessToOtherPlots.entrySet()) {
					entry.getValue().mProfile.complete();
				}

				Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
					/* Switch back to the main thread to finish assembling all the heads */
					for (Map.Entry<UUID, OtherAccessRecord> entry : mOwnerAccessToOtherPlots.entrySet()) {
						OtherAccessRecord rec = entry.getValue();

						ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
						SkullMeta meta = (SkullMeta) head.getItemMeta();
						meta.setPlayerProfile(rec.mProfile);
						meta.displayName(Component.text(rec.mName + "'s Plot", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
						List<Component> lore = new ArrayList<Component>();
						lore.add(Component.text("Access expires in:", NamedTextColor.GRAY)
								.decoration(TextDecoration.ITALIC, false));
						String timeLeft = (rec.mExpiration == -1) ? "Unlimited" : MessagingUtils.getTimeDifferencePretty(rec.mExpiration);
						lore.add(Component.text(timeLeft, NamedTextColor.GRAY)
								.decoration(TextDecoration.ITALIC, false));
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
	private static CompletableFuture<PlotInfo> getPlotInfo(UUID ownerUUID) {
		CompletableFuture<PlotInfo> future = new CompletableFuture<>();

		final int plot;
		final int currentPlot;
		final CompletableFuture<Map<String, Integer>> scoresFuture;

		Player owner = Bukkit.getPlayer(ownerUUID);
		if (owner != null) {
			plot = ScoreboardUtils.getScoreboardValue(owner, "Plot").orElse(0);
			currentPlot = ScoreboardUtils.getScoreboardValue(owner, "CurrentPlot").orElse(0);
			scoresFuture = null;
		} else {
			// These two values will be overwritten by the scores future
			plot = 0;
			currentPlot = 0;
			scoresFuture = MonumentaRedisSyncAPI.getPlayerScores(ownerUUID);
		}

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				Map<String, String> allRemoteData = MonumentaRedisSyncAPI.getAllRemoteData(ownerUUID).get();

				/* If the player wasn't online, fetching scores already started, need to complete that and get the specific interesting values */
				final int finalPlot;
				final int finalCurrentPlot;
				if (scoresFuture != null) {
					Map<String, Integer> scores = scoresFuture.get();
					finalPlot = scores.getOrDefault("Plot", 0);
					finalCurrentPlot = scores.getOrDefault("CurrentPlot", 0);
				} else {
					finalPlot = plot;
					finalCurrentPlot = currentPlot;
				}

				Map<UUID, Long> otherAccessToOwnerPlot = new HashMap<>();
				Map<UUID, PlotInfo.OtherAccessRecord> ownerAccessToOtherPlots = new HashMap<>();

				/* Filter the entries into other access to plot, self access to other plots, and remove expired entries */
				allRemoteData.forEach((key, value) -> {
					if (key.startsWith("myplotaccess|") || key.startsWith("otherplotaccess|")) {
						UUID otherUUID;
						long expiration;
						if (key.startsWith("myplotaccess|")) {
							otherUUID = UUID.fromString(key.substring("myplotaccess|".length()));
							expiration = Long.parseLong(value);
							if (plotAccessIsExpired(expiration)) {
								plotAccessRemove(ownerUUID, otherUUID);
							} else {
								otherAccessToOwnerPlot.put(otherUUID, expiration);
							}
						} else {
							otherUUID = UUID.fromString(key.substring("otherplotaccess|".length()));
							String[] split = value.split(",");
							int plotId = Integer.parseInt(split[0]);
							expiration = Long.parseLong(split[1]);
							if (plotAccessIsExpired(expiration)) {
								plotAccessRemove(otherUUID, ownerUUID);
							} else {
								ownerAccessToOtherPlots.put(otherUUID, new PlotInfo.OtherAccessRecord(plotId, expiration));
							}
						}
					}
				});

				PlotInfo info = new PlotInfo(ownerUUID, finalPlot, finalCurrentPlot, otherAccessToOwnerPlot, ownerAccessToOtherPlots);

				/* Complete future on main thread for easy use of .whenCompleted() */
				Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
					future.complete(info);
				});
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
					future.completeExceptionally(ex);
				});
			}
		});

		return future;
	}
}

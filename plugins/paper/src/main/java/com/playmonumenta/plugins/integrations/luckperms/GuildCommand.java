package com.playmonumenta.plugins.integrations.luckperms;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.integrations.luckperms.listeners.InviteNotification;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.shardhealth.ShardHealthManager;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.worlds.paper.MonumentaWorldManagementAPI;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GuildCommand {
	public static boolean HAS_EXPORTED_THIS_RESTART = false;
	public static boolean HAS_IMPORTED_THIS_RESTART = false;

	public static void register(Plugin plugin) {
		CommandAPICommand root = new CommandAPICommand("guild");
		CommandAPICommand modOnly = new CommandAPICommand("mod");
		CommandAPICommand testSubcommand = new CommandAPICommand("test");

		attach(root);
		attachModOnly(plugin, modOnly);
		GuildTestCommand.attach(testSubcommand);
		modOnly.withSubcommands(
			ChangeGuildBanner.attach(plugin, new CommandAPICommand("setbanner")),
			ChangeGuildColor.attach(plugin, new CommandAPICommand("color")),
			DeleteGuildCommand.attach(plugin, new CommandAPICommand("delete")),
			RenameGuild.attach(plugin, new CommandAPICommand("rename")),
			GuildChatColorCommand.attachModOnly(plugin, new CommandAPICommand("chatcolor"))
		);

		for (GuildAccessLevel accessLevel : GuildAccessLevel.values()) {
			modOnly.withSubcommands(
				GuildAccessCommand.attachModOnly(plugin, new CommandAPICommand("access"), accessLevel)
			);
			root.withSubcommands(
				GuildAccessCommand.attach(plugin, new CommandAPICommand("access"), accessLevel)
			);
		}

		for (GuildInviteLevel inviteLevel : GuildInviteLevel.values()) {
			modOnly.withSubcommands(
				GuildInviteCommand.attachModOnly(plugin, new CommandAPICommand("invite"), inviteLevel)
			);
			root.withSubcommands(
				GuildInviteCommand.attach(plugin, new CommandAPICommand("invite"), inviteLevel)
			);
		}


		root.withSubcommands(
			GuildChatColorCommand.attach(plugin, new CommandAPICommand("chatcolor")),
			testSubcommand, // Register commands for mechanisms
			modOnly // Register mod-only commands
		);
		root.register();
	}

	protected static final Argument<String> GUILD_NAME_ARG = new TextArgument("guild name")
		.replaceSuggestions(GuildArguments.NAME_SUGGESTIONS);
	private static final CommandPermission REBUILD_PERMISSION
		= CommandPermission.fromString("monumenta.command.guild.mod.rebuildtag");
	private static final CommandPermission EXPORT_GUILD_PLOTS_PERMISSION
		= CommandPermission.fromString("monumenta.command.guild.mod.exportguildplots");
	private static final CommandPermission IMPORT_GUILD_PLOTS_PERMISSION
		= CommandPermission.fromString("monumenta.command.guild.mod.importguildplots");

	private static CommandAPICommand attach(CommandAPICommand root) {
		//used to attach a few small commands that don't need their own file.

		CommandAPICommand inviteCheckSub = new CommandAPICommand("invitecheck");
		inviteCheckSub
			.executes((sender, args) -> {
				if (senderCannotRunCommand(sender, false)) {
					return;
				}

				CommandSender callee = CommandUtils.getCallee(sender);
				if (!(callee instanceof Player player)) {
					throw CommandAPI.failWithString("This command must be run as a player");
				}

				InviteNotification.notifyInvitedLogin(player);
			});

		return root.withSubcommands(
			inviteCheckSub
		);
	}

	private static CommandAPICommand attachModOnly(Plugin plugin, CommandAPICommand root) {
		//used to attach a few small commands that don't need their own file.

		CommandAPICommand rebuildTagSub = new CommandAPICommand("rebuildtag");
		rebuildTagSub
			.withArguments(GUILD_NAME_ARG)
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, REBUILD_PERMISSION);
				if (senderCannotRunCommand(sender, true)) {
					return;
				}

				String guildName = args.getByArgument(GUILD_NAME_ARG);
				String rootId = GuildArguments.getIdFromName(guildName);
				if (rootId == null) {
					throw CommandAPI.failWithString("Could not identify guild by name: " + guildName);
				}

				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
					Group guild = LuckPermsIntegration.GM.loadGroup(rootId).join().orElse(null);
					Group rootGuild = LuckPermsIntegration.getGuildRoot(guild);
					if (rootGuild == null) {
						Bukkit.getScheduler().runTask(plugin,
							() -> sender.sendMessage(Component.text("Could not rebuild tag, as guild '"
								+ rootId + "' does not exist.", NamedTextColor.RED)));
						return;
					}

					try {
						LuckPermsIntegration.rebuildTag(sender, rootGuild).join();
					} catch (IllegalStateException ex) {
						Bukkit.getScheduler().runTask(plugin, () -> {
							MMLog.warning("Could not rebuild tag of '" + rootId + "' due to: ", ex);
							sender.sendMessage(Component.text("Failed to rebuild tag for '"
								+ rootId + "'", NamedTextColor.RED));
						});
						return;
					}

					//push update
					LuckPermsIntegration.pushUpdate();
					Bukkit.getScheduler().runTask(plugin, () -> AuditListener.log("<+> Finished updating tag for '"
						+ rootId + "'\nTask executed by: " + sender.getName())
					);
				});
			});

		CommandAPICommand exportGuildPlotsSub = exportGuildPlotsSub(plugin);
		CommandAPICommand importGuildPlotsSub = importGuildPlotsSub(plugin);

		return root
			.withSubcommands(
				rebuildTagSub
			)
			.withSubcommands(
				exportGuildPlotsSub,
				importGuildPlotsSub
			);
	}

	private static @NotNull CommandAPICommand exportGuildPlotsSub(Plugin plugin) {
		CommandAPICommand exportGuildPlotsSub = new CommandAPICommand("exportguildplots");
		exportGuildPlotsSub
			.executesPlayer((player, args) -> {
				CommandUtils.checkPerm(player, EXPORT_GUILD_PLOTS_PERMISSION);
				if (senderCannotRunCommand(player, true)) {
					return;
				}

				if (HAS_EXPORTED_THIS_RESTART) {
					player.sendMessage(Component.text(
						"An export attempt has already been started. If another export is required, restart the shard first.",
						NamedTextColor.RED
					));
					return;
				}
				HAS_EXPORTED_THIS_RESTART = true;

				if (!"plots".equals(ServerProperties.getShardName())) {
					throw CommandAPI.failWithString("This command only works on the plots shard.");
				}

				final World originalPlotsWorld = Bukkit.getWorlds().get(0);
				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
					List<Group> guilds = LuckPermsIntegration.getGuilds().join();
					int numGuilds = guilds.size();

					JsonObject allPlotBounds = new JsonObject();

					int guildNum = 0;
					boolean foundProblems = false;
					for (Group guild : guilds) {
						guildNum++;

						ShardHealthManager.awaitShardHealth(player).join();

						PlotsGuildBounds plotsGuildBounds
							= PlotsGuildBounds.getGuildBounds(originalPlotsWorld, guild).join();

						ShardHealthManager.awaitShardHealth(player).join();

						String guildName = LuckPermsIntegration.getNonNullGuildName(guild);
						if (plotsGuildBounds == null) {
							player.sendMessage(Component.text("- " + guildNum + "/" + numGuilds
								+ ": " + guildName + " has no guild plot", NamedTextColor.GRAY));
							continue;
						}

						int numGuildIslands = plotsGuildBounds.islandBbs().size();
						boolean isProblem = numGuildIslands > 1;
						foundProblems |= isProblem;

						JsonArray guildMembers = new JsonArray();
						Group guildRoot = LuckPermsIntegration.getGuildRoot(guild);
						Set<UUID> memberIds;
						if (guildRoot == null) {
							memberIds = new HashSet<>();
						} else {
							memberIds = LuckPermsIntegration.getAllGuildMembers(guildRoot, true).join();
						}
						for (UUID memberId : memberIds) {
							String memberName = MonumentaRedisSyncIntegration.cachedUuidToName(memberId);
							if (memberName != null) {
								guildMembers.add(memberName);
							}
						}

						player.sendMessage(Component.text("- " + guildNum + "/" + numGuilds
								+ ": " + guildName + " (plot " + plotsGuildBounds.plotNumber()
								+ ") has " + numGuildIslands + " guild island(s); " + memberIds.size() + " members, " + guildMembers.size() + " have names",
							isProblem ? NamedTextColor.RED : NamedTextColor.GREEN));

						JsonObject boundsJson = plotsGuildBounds.toJson();
						boundsJson.add("members", guildMembers);
						allPlotBounds.add(guildName, boundsJson);

						try {
							plotsGuildBounds.saveStructures(player, originalPlotsWorld).join();
						} catch (CompletionException ex) {
							MessagingUtils.sendStackTrace(player, ex);
						}
					}

					try {
						FileUtils.writeJsonSafely(
							Plugin.getInstance().getDataFolder() + "/plots_guild_bounds.json",
							allPlotBounds,
							false
						);
					} catch (IOException e) {
						player.sendMessage(Component.text("Failed to write plots guild bounds json", NamedTextColor.RED));
					}
					player.sendMessage(Component.text("Done!", foundProblems ? NamedTextColor.RED : NamedTextColor.GREEN));
				});
			});
		return exportGuildPlotsSub;
	}

	// The review dog doesn't correctly detect changes from sync to async code and vice versa here
	@SuppressWarnings("UnnecessaryAsync")
	private static @NotNull CommandAPICommand importGuildPlotsSub(Plugin plugin) {
		CommandAPICommand importGuildPlotsSub = new CommandAPICommand("importguildplots");
		importGuildPlotsSub
			.withArguments(new EntitySelectorArgument.ManyPlayers("volunteers"))
			.executesPlayer((player, args) -> {
				Collection<Player> volunteers = Objects.requireNonNull(args.getUnchecked("volunteers"));
				Set<Audience> allAudiences = new HashSet<>();
				allAudiences.add(Bukkit.getConsoleSender());
				for (Player volunteer : volunteers) {
					allAudiences.add(volunteer);
					volunteer.setGameMode(GameMode.SPECTATOR);
				}
				Audience volunteerAudience = Audience.audience(allAudiences);

				CommandUtils.checkPerm(player, IMPORT_GUILD_PLOTS_PERMISSION);
				if (senderCannotRunCommand(player, true)) {
					return;
				}

				if (!GuildPlotUtils.SHARD_NAME.equals(ServerProperties.getShardName())) {
					throw CommandAPI.failWithString("This command only works on the guildplots shard.");
				}

				if (HAS_IMPORTED_THIS_RESTART) {
					volunteerAudience.sendMessage(Component.text(
						"An import attempt has already been started. If another import is required, restart the shard first.",
						NamedTextColor.RED
					));
					return;
				}
				HAS_IMPORTED_THIS_RESTART = true;

				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
					JsonObject allPlotBoundsJson;
					try {
						allPlotBoundsJson = FileUtils.readJson(Plugin.getInstance().getDataFolder() + "/plots_guild_bounds.json");
					} catch (Exception ignored) {
						volunteerAudience.sendMessage(Component.text(
							"Unable to load exported plot bounds json file; was it copied to this shard?",
							NamedTextColor.RED
						));
						return;
					}

					int numGuildPlots = allPlotBoundsJson.size();
					LinkedBlockingQueue<PlotsGuildBounds> remainingGuilds = new LinkedBlockingQueue<>();
					for (Map.Entry<String, JsonElement> guildEntry : allPlotBoundsJson.entrySet()) {
						String guildName = guildEntry.getKey();
						JsonElement plotsGuildBoundsJson = guildEntry.getValue();

						try {
							remainingGuilds.add(PlotsGuildBounds.fromJson(guildName, plotsGuildBoundsJson));
						} catch (Exception ex) {
							volunteerAudience.sendMessage(Component.text(
								"Error parsing guild bounds for " + guildName,
								NamedTextColor.RED
							));
							MessagingUtils.sendStackTrace(volunteerAudience, ex);
							break;
						}
					}

					AtomicInteger startedGuildPlots = new AtomicInteger(0);
					Map<UUID, CompletableFuture<Void>> volunteerFutures = new ConcurrentHashMap<>();
					for (Player volunteer : volunteers) {
						CompletableFuture<Void> volunteerFuture = new CompletableFuture<>();
						volunteerFutures.put(volunteer.getUniqueId(), volunteerFuture);

						Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
							// Ignoring the starting world, we want to keep the current world loaded
							AtomicReference<String> currentWorld = new AtomicReference<>();
							// ...and we want to unload the previous world if it's not the starting world
							// We can keep going while unloading that world...
							AtomicReference<CompletableFuture<Void>> recentlyUnloadingWorld = new AtomicReference<>();
							// ...but we should really avoid leaving too many around, so wait for the one before that
							AtomicReference<CompletableFuture<Void>> olderUnloadingWorld = new AtomicReference<>();

							while (true) {
								PlotsGuildBounds guildBounds = remainingGuilds.poll();
								if (guildBounds == null) {
									volunteerFuture.complete(null);
									return;
								}

								ShardHealthManager.awaitShardHealth(volunteerAudience).join();

								while (true) {
									CompletableFuture<Void> unloadFuture = olderUnloadingWorld.get();
									if (unloadFuture == null) {
										break;
									} else if (unloadFuture.isDone()) {
										olderUnloadingWorld.set(null);
										break;
									} else {
										volunteerAudience.sendActionBar(Component.text(
											"Delaying due to old worlds unloading",
											NamedTextColor.YELLOW
										));
									}

									CompletableFuture<Void> delayThreadFuture = new CompletableFuture<>();
									Bukkit.getScheduler().runTaskLater(plugin,
										() -> delayThreadFuture.complete(null), 10L);
									delayThreadFuture.join();
								}

								ShardHealthManager.awaitShardHealth(volunteerAudience).join();

								int i = startedGuildPlots.addAndGet(1);
								String guildName = guildBounds.mGuildName;
								volunteerAudience.sendMessage(Component.text(
									"[" + i + "/" + numGuildPlots
										+ "] \"" + guildName + "\" is loading...",
									NamedTextColor.GRAY
								));

								try {
									guildBounds.loadStructures(volunteerAudience, volunteer).join();
								} catch (CompletionException ex) {
									Exception exception
										= new Exception("Error pasting guild for " + guildName, ex.getCause());
									volunteerFuture.completeExceptionally(exception);
									break;
								}

								ShardHealthManager.awaitShardHealth(volunteerAudience).join();

								String previousWorld = currentWorld.get();
								currentWorld.set(volunteer.getWorld().getName());
								if (previousWorld != null) {
									CompletableFuture<Void> unloadFuture = new CompletableFuture<>();
									Bukkit.getScheduler().runTask(plugin, () -> {
										olderUnloadingWorld.set(recentlyUnloadingWorld.get());
										recentlyUnloadingWorld.set(
											MonumentaWorldManagementAPI.unloadWorld(previousWorld));
										unloadFuture.complete(null);
									});
									unloadFuture.join();
								}

								ShardHealthManager.awaitShardHealth(volunteerAudience).join();
							}
						});
					}

					for (CompletableFuture<Void> volunteerFuture : volunteerFutures.values()) {
						try {
							volunteerFuture.join();
						} catch (CompletionException ex) {
							MessagingUtils.sendStackTrace(volunteerAudience, ex);
							break;
						}
					}

					volunteerAudience.sendMessage(Component.text("Done!", NamedTextColor.GREEN));
					for (Player volunteer : volunteers) {
						GuildPlotUtils.sendGuildPlotFallbackWorld(volunteer, false);
						volunteer.setGameMode(GameMode.ADVENTURE);
						volunteer.playSound(Sound.sound(
							Key.key("minecraft:music_disc.ward"),
							Sound.Source.RECORD,
							1.0f,
							1.0f
						));
					}
				});
			});
		return importGuildPlotsSub;
	}

	public static boolean senderCannotRunCommand(CommandSender sender, boolean needOp)
		throws WrapperCommandSyntaxException {
		if (ServerProperties.getShardName().equals("build")) {
			throw CommandAPI.failWithString("This command cannot be run on the build shard.");
		}

		if (!sender.hasPermission("monumenta.command.guild.mod") && needOp) {
			throw CommandAPI.failWithString("This command may only be run by an operator.");
		}

		return false;
	}
}

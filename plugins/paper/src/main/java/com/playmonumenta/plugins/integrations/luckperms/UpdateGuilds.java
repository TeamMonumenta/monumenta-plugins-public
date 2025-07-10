package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.shardhealth.ShardHealthManager;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.worlds.paper.MonumentaWorldManagementAPI;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

public class UpdateGuilds {
	private static final List<GuildPermission> mNewPermissions = List.of(
	);

	private static final List<FillArea> mFillAreas = List.of(
		// Fill Northern section all the way across
		new FillArea(-384, -384, 383, -228),
		// Fill Southern section all the way across
		new FillArea(-384, 228, 383, 383),

		// Fill Western section interior
		new FillArea(-384, -228, -228, 228),
		// Fill Eastern section interior
		new FillArea(228, -228, 383, 228)
	);

	public static void register(Plugin plugin) {
		// guild mod update
		CommandPermission perms = CommandPermission.fromString("monumenta.command.guild.mod.update");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("mod"));
		arguments.add(new LiteralArgument("update"));

		new CommandAPICommand("guild")
			.withArguments(arguments)
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, perms);
				if (ServerProperties.getShardName().contains("build")) {
					throw CommandAPI.failWithString("This command cannot be run on the build shard.");
				}

				run(plugin, sender);
			})
			.register();
	}

	public static void run(Plugin plugin, CommandSender sender) {
		if (!ServerProperties.getShardName().equals("guildplots")) {
			sender.sendMessage(Component.text("This needs to be run on the guildplots shard", NamedTextColor.RED));
			return;
		}

		List<Group> guilds;
		try {
			guilds = LuckPermsIntegration.getGuilds().join();
		} catch (Exception ex) {
			sender.sendMessage(Component.text("Failed to get list of guilds:", NamedTextColor.RED));
			MessagingUtils.sendStackTrace(sender, ex);
			return;
		}

		for (Group memberGroup : guilds) {
			Group guildRootGroup = LuckPermsIntegration.getGuildRoot(memberGroup);
			if (guildRootGroup == null) {
				sender.sendMessage(Component.text("Failed to identify root of " + memberGroup.getFriendlyName(), NamedTextColor.RED));
				continue;
			}
			String guildRootGroupId = guildRootGroup.getName();
			try {
				for (GuildAccessLevel accessLevel : List.of(
					GuildAccessLevel.FOUNDER,
					GuildAccessLevel.MANAGER,
					GuildAccessLevel.MEMBER
				)) {
					Group accessGroup = accessLevel.loadGroupFromRoot(guildRootGroup).join().orElse(null);
					if (accessGroup == null) {
						sender.sendMessage(Component.text("- Could not find " + accessLevel.mId + " group for " + guildRootGroupId));
						continue;
					}

					for (GuildPermission guildPermission : mNewPermissions) {
						if (accessLevel.compareTo(guildPermission.mDefaultAccessLevel) <= 0) {
							guildPermission.setExplicitPermission(guildRootGroup, accessGroup, true).join();
						}
					}
				}

				Group blockedGroup = GuildAccessLevel.BLOCKED.loadGroupFromRoot(guildRootGroup).join().orElse(null);
				if (blockedGroup == null) {
					sender.sendMessage(Component.text("- Could not find blocked group for " + guildRootGroupId));
				} else {
					for (GuildPermission guildPermission : mNewPermissions) {
						guildPermission.setExplicitPermission(guildRootGroup, blockedGroup, false).join();
					}
				}

				sender.sendMessage(Component.text("Updated " + guildRootGroupId, NamedTextColor.GREEN));
			} catch (Exception ex) {
				sender.sendMessage(Component.text("Failed to update " + guildRootGroupId + ":", NamedTextColor.RED));
				MessagingUtils.sendStackTrace(sender, ex);
			}
		}

		// -384 to -228, 228 to 383
		// -384,0,-384
		// 383,511,-228

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			// Get sorted list for a sense of how far along we are
			NavigableSet<Long> plotNumbers = new TreeSet<>();
			for (String worldName : MonumentaWorldManagementAPI.getAvailableWorlds()) {
				Long plotNumber = GuildPlotUtils.getGuildPlotNumber(worldName);
				if (plotNumber != null) {
					plotNumbers.add(plotNumber);
				}
			}

			for (Long plotNumber : plotNumbers.descendingSet()) {
				String worldName = GuildPlotUtils.guildPlotName(plotNumber);
				if (GuildPlotUtils.getGuildPlotNumber(worldName) == null) {
					// Not a guild plot; probably a template world
					continue;
				}

				ShardHealthManager.awaitShardHealth(sender).join();

				// Load the next guild plot world
				CompletableFuture<World> worldLoadFuture = new CompletableFuture<>();
				Bukkit.getScheduler().runTask(plugin, () -> {
					try {
						worldLoadFuture.complete(MonumentaWorldManagementAPI.ensureWorldLoaded(worldName, null));
					} catch (Exception ex) {
						worldLoadFuture.completeExceptionally(ex);
					}
				});

				World world;
				try {
					world = worldLoadFuture.join();
				} catch (CompletionException ex) {
					sender.sendMessage(Component.text("Unable to load " + worldName, NamedTextColor.RED));
					MessagingUtils.sendStackTrace(sender, ex);
					continue;
				}

				try {
					fillAreas(sender, world).join();
				} catch (CompletionException ex) {
					sender.sendMessage(Component.text("Unable to fill areas in " + worldName, NamedTextColor.RED));
					MessagingUtils.sendStackTrace(sender, ex);
					continue;
				}

				// Save and unload
				ShardHealthManager.awaitShardHealth(sender).join();
				CompletableFuture<Void> saveAndUnloadFuture = new CompletableFuture<>();
				Bukkit.getScheduler().runTask(plugin, () -> {
					try {
						world.save();

						// Unload
						if (world.getPlayers().isEmpty()) {
							MonumentaWorldManagementAPI.unloadWorld(worldName);
						}

						saveAndUnloadFuture.complete(null);
					} catch (Exception ex) {
						saveAndUnloadFuture.completeExceptionally(ex);
					}
				});
				try {
					saveAndUnloadFuture.join();
				} catch (CompletionException ex) {
					sender.sendMessage(Component.text("Unable to save or unload " + worldName, NamedTextColor.RED));
					MessagingUtils.sendStackTrace(sender, ex);
					continue;
				}

				sender.sendMessage(Component.text("Updated " + worldName, NamedTextColor.GOLD));
			}

			sender.sendMessage(Component.text("Done!", NamedTextColor.GREEN));
		});
	}

	private static CompletableFuture<Void> fillAreas(CommandSender sender, World world) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		ShardHealthManager.awaitShardHealth(sender).join();

		BukkitWorld bukkitWorld = new BukkitWorld(world);
		int minY = world.getMinHeight();
		int maxY = world.getMaxHeight();

		try (
			EditSession editSession = WorldEdit.getInstance()
				.newEditSessionBuilder()
				.world(bukkitWorld)
				.fastMode(true)
				.combineStages(true)
				.changeSetNull()
				.checkMemory(false)
				.allowedRegionsEverywhere()
				.limitUnlimited()
				.build()
		) {
			BlockState barrierBlockState = BukkitAdapter.adapt(Material.BARRIER.createBlockData());

			for (FillArea fillArea : mFillAreas) {
				BlockVector3 minCorner = BlockVector3.at(fillArea.minX, minY, fillArea.minZ);
				BlockVector3 maxCorner = BlockVector3.at(fillArea.maxX, maxY, fillArea.maxZ);
				CuboidRegion region = new CuboidRegion(bukkitWorld, minCorner, maxCorner);
				editSession.setBlocks((Region) region, barrierBlockState);
			}

			editSession.flushQueue();
			future.complete(null);
		} catch (Exception ex) {
			future.completeExceptionally(ex);
		}

		return future;
	}

	private record FillArea(int minX, int minZ, int maxX, int maxZ) {
	}
}

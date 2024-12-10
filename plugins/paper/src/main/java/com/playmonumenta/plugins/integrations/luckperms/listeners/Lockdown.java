package com.playmonumenta.plugins.integrations.luckperms.listeners;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.integrations.luckperms.TeleportGuildGui;
import com.playmonumenta.plugins.plots.ShopManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.event.sync.PostSyncEvent;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.MetaNode;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.GUILD_ROOT_LOCKDOWN_MK;
import static com.playmonumenta.plugins.integrations.luckperms.OffDutyCommand.ON_DUTY_PERM_STRING;

public class Lockdown implements Listener {
	// The presence of a map indicates a guild is locked, even if the map is empty (for non-plots shards)
	private static final Map<String, Map<UUID, List<BoundingBox>>> mLockedDownAreas = new HashMap<>();
	private static final Set<UUID> mTeleportingPlayers = new HashSet<>();

	public Lockdown() {
	}

	public static void registerLuckPermsEvents(Plugin plugin, EventBus eventBus) {
		eventBus.subscribe(plugin, NodeAddEvent.class, Lockdown::nodeAddEvent);
		eventBus.subscribe(plugin, NodeRemoveEvent.class, Lockdown::nodeRemoveEvent);
		eventBus.subscribe(plugin, PostSyncEvent.class, Lockdown::postSyncEvent);

		MMLog.fine("[Guild Lockdown Kicker] Getting guilds list async...");
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			List<Group> guilds = LuckPermsIntegration.getGuilds().join();
			afterLoad(plugin, guilds);
		});
	}

	// Guilds are checked a tick at a time to reduce the impact of loading too much of the plots shard
	private static void afterLoad(Plugin plugin, List<Group> guilds) {
		MMLog.fine("[Guild Lockdown Kicker] Checking if plots...");
		final World originalPlotsWorld;
		if ("plots".equals(ServerProperties.getShardName())) {
			MMLog.fine("[Guild Lockdown Kicker] Plots shard confirmed...");
			originalPlotsWorld = Bukkit.getWorlds().get(0);
		} else {
			originalPlotsWorld = null;
		}
		final int numGuilds = guilds.size();
		MMLog.info("[Guild Lockdown Kicker] Going through " + numGuilds + " guilds...");
		Iterator<Group> guildIterator = guilds.listIterator();
		new BukkitRunnable() {
			int mIterations = 0;

			@Override
			public void run() {
				final Group guild;
				Group testGuild;
				while (true) {
					if (!guildIterator.hasNext()) {
						MMLog.info("[Guild Lockdown Kicker] (" + mIterations + "/" + numGuilds + ") finished loading.");
						cancel();
						return;
					}
					testGuild = guildIterator.next();

					mIterations++;
					if (!LuckPermsIntegration.isLocked(testGuild)) {
						MMLog.info("[Guild Lockdown Kicker] (" + mIterations + "/" + numGuilds + ") unlocked, skipping...");
					} else {
						MMLog.info("[Guild Lockdown Kicker] (" + mIterations + "/" + numGuilds + ") locked, processing...");
						break;
					}
				}
				guild = testGuild;

				Map<UUID, List<BoundingBox>> lockedGuildAreas = new HashMap<>();
				mLockedDownAreas.put(guild.getName(), lockedGuildAreas);
				if (originalPlotsWorld != null) {
					final int currentIter = mIterations;
					MMLog.info("[Guild Lockdown Kicker] (" + currentIter + "/" + numGuilds
						+ ") Checking guild plot locations...");
					Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
						List<BoundingBox> boundingBoxes
							= getOriginalGuildPlotAndIslands(originalPlotsWorld, guild, true).join();
						Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
							lockedGuildAreas.put(originalPlotsWorld.getUID(), boundingBoxes);
							MMLog.info("[Guild Lockdown Kicker] (" + currentIter + "/" + numGuilds
								+ ") Registered guild plot locations...");
						});
					});
				}
			}
		}.runTaskTimer(plugin, 0, 1);
	}

	// Local events
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		checkPlayerLockout(event.getPlayer(), true);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerTeleportEvent(PlayerTeleportEvent event) {
		final Player player = event.getPlayer();
		if (!mTeleportingPlayers.remove(player.getUniqueId())) {
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> checkPlayerLockout(player, false));
		}
	}

	public static void nodeAddEvent(NodeAddEvent event) {
		PermissionHolder permissionHolder = event.getTarget();
		if (!(permissionHolder instanceof Group group)) {
			return;
		}

		Node node = event.getNode();
		if (!(node instanceof MetaNode metaNode)) {
			return;
		}

		if (!metaNode.getMetaKey().equals(GUILD_ROOT_LOCKDOWN_MK)) {
			return;
		}

		MMLog.info("[Guild Lockdown Kicker] Got lockdown start event");
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> onLockdownStart(group));
	}

	public static void nodeRemoveEvent(NodeRemoveEvent event) {
		PermissionHolder permissionHolder = event.getTarget();
		if (!(permissionHolder instanceof Group group)) {
			return;
		}

		Node node = event.getNode();
		if (!(node instanceof MetaNode metaNode)) {
			return;
		}

		if (!metaNode.getMetaKey().equals(GUILD_ROOT_LOCKDOWN_MK)) {
			return;
		}

		MMLog.info("[Guild Lockdown Kicker] Got lockdown end event");
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> onLockdownEnd(group));
	}

	// Remote events
	public static void postSyncEvent(PostSyncEvent event) {
		MMLog.fine("[Guild Lockdown Kicker] Got post sync event");
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			List<Group> guilds = LuckPermsIntegration.getGuilds().join();

			// Identify and delete guild lockdown info for guilds that no longer exist (no need to alert anyone)
			Set<String> guildIds = new HashSet<>();
			for (Group guild : guilds) {
				guildIds.add(guild.getName());
			}
			Set<String> deletedGuilds = new HashSet<>(mLockedDownAreas.keySet());
			deletedGuilds.removeAll(guildIds);
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				for (String deletedGuild : deletedGuilds) {
					mLockedDownAreas.remove(deletedGuild);
				}
			});

			// The rest of these sort out locked/unlocked state as needed
			for (Group guild : guilds) {
				Map<UUID, List<BoundingBox>> lockedAreas = mLockedDownAreas.get(guild.getName());

				boolean wasLocked = lockedAreas != null;
				boolean isLocked = LuckPermsIntegration.isLocked(guild);

				// Unlock guilds that are no longer locked
				if (wasLocked && !isLocked) {
					Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> onLockdownEnd(guild));
				}

				// Lock guilds that have become locked
				if (!wasLocked && isLocked) {
					Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> onLockdownStart(guild));
				}
			}
		});
	}

	private static void onLockdownStart(Group guild) {
		Map<UUID, List<BoundingBox>> lockedGuildAreas = new HashMap<>();
		mLockedDownAreas.put(guild.getName(), lockedGuildAreas);

		boolean isOriginalPlotsShard = "plots".equals(ServerProperties.getShardName());
		World originalPlotsWorld;
		if (isOriginalPlotsShard) {
			originalPlotsWorld = Bukkit.getWorlds().get(0);

			lockOriginalGuildMarketPlot(originalPlotsWorld, guild);

			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				List<BoundingBox> boundingBoxes
					= getOriginalGuildPlotAndIslands(originalPlotsWorld, guild, true).join();
				Bukkit.getScheduler().runTask(Plugin.getInstance(),
					() -> lockedGuildAreas.put(originalPlotsWorld.getUID(), boundingBoxes));
			});
		}

		Component lockdownMessage = getLockdownMessage(guild);
		Bukkit.getConsoleSender().sendMessage(lockdownMessage);
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.hasPermission(ON_DUTY_PERM_STRING)) {
				player.sendMessage(lockdownMessage);
				player.playSound(player,
					Sound.ITEM_TOTEM_USE,
					SoundCategory.PLAYERS,
					0.7f,
					Constants.Note.FS4.mPitch);
			} else {
				checkPlayerLockout(player, guild, true);
			}
		}
	}

	private static void onLockdownEnd(Group guild) {
		mLockedDownAreas.remove(guild.getName());
		Component unlockMessage = Component.text("A moderator has ended the lockdown for the guild ", NamedTextColor.GOLD)
			.append(Component.text("", NamedTextColor.WHITE)
				.append(LuckPermsIntegration.getGuildFullComponent(guild)))
			.append(Component.text(". You now have access again."));

		Bukkit.getConsoleSender().sendMessage(unlockMessage);
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (!player.hasPermission("group." + guild.getName())) {
				continue;
			}
			player.sendMessage(unlockMessage);
			player.playSound(player,
				Sound.BLOCK_IRON_DOOR_CLOSE,
				SoundCategory.PLAYERS,
				0.7f,
				Constants.Note.FS3.mPitch);
		}
	}

	private static void lockOriginalGuildMarketPlot(World originalPlotsWorld, Group guild) {
		// Guild market plot (no teleport required, just lock it; assumes this only runs on the correct shard)
		String guildMarketPlotTag = "shop_ownerGuildName=" + LuckPermsIntegration.getNonNullGuildName(guild);
		for (int x = -43; x <= -39; x++) {
			for (int z = 65; z <= 68; z++) {
				Chunk chunk = originalPlotsWorld.getChunkAt(x, z);
				for (Entity entity : chunk.getEntities()) {
					if (!entity.getType().equals(EntityType.SHULKER)) {
						continue;
					}
					if (!entity.getScoreboardTags().contains(guildMarketPlotTag)) {
						continue;
					}
					try {
						ShopManager.setLockable(entity, null, true);
						ShopManager.shopLock(entity, null, true);
					} catch (Exception ex) {
						Location shopLoc = entity.getLocation();
						MonumentaNetworkRelayIntegration.sendAuditLogSevereMessage("Unable to lock guild market plot for "
							+ MessagingUtils.plainText(LuckPermsIntegration.getGuildFullComponent(guild))
							+ " at " + shopLoc.getBlockX()
							+ " " + shopLoc.getBlockY()
							+ " " + shopLoc.getBlockZ());
					}
				}
			}
		}
	}

	private static Component getLockdownMessage(Group guild) {
		return Component.text("The guild ", NamedTextColor.GOLD)
			.append(Component.text("", NamedTextColor.WHITE)
				.append(LuckPermsIntegration.getGuildFullComponent(guild)))
			.append(Component.text(" started an emergency lockdown. Moderator help has been requested."));
	}

	private static void checkPlayerLockout(Player player, boolean hasNotSeenMessage) {
		for (Group guild : LuckPermsIntegration.getRelevantGuilds(player, true, true)) {
			Group guildRoot = LuckPermsIntegration.getGuildRoot(guild);
			if (guildRoot == null) {
				continue;
			}
			checkPlayerLockout(player, guildRoot, hasNotSeenMessage);
		}
	}

	private static void checkPlayerLockout(Player player, Group guild, boolean hasNotSeenMessage) {
		Map<UUID, List<BoundingBox>> lockedAreas = mLockedDownAreas.get(guild.getName());
		if (lockedAreas == null) {
			// This must be empty but not null if the guild is locked
			return;
		}

		boolean requiresMessage = hasNotSeenMessage && player.hasPermission("group." + guild.getName());
		boolean requiresTp = false;
		Location playerLocation = player.getLocation();
		Location spawnLocation = player.getRespawnLocation();

		Set<UUID> checkedWorldIds = new HashSet<>();
		checkedWorldIds.add(playerLocation.getWorld().getUID());
		if (spawnLocation != null) {
			checkedWorldIds.add(spawnLocation.getWorld().getUID());
		}

		for (Map.Entry<UUID, List<BoundingBox>> lockedAreaEntry : lockedAreas.entrySet()) {
			UUID worldId = lockedAreaEntry.getKey();
			if (!checkedWorldIds.contains(worldId)) {
				continue;
			}

			for (BoundingBox guildPlotOrIsland : lockedAreaEntry.getValue()) {
				if (worldId.equals(playerLocation.getWorld().getUID())
					&& guildPlotOrIsland.overlaps(player.getBoundingBox())) {
					requiresTp = true;
				}

				if (spawnLocation != null
					&& worldId.equals(spawnLocation.getWorld().getUID())
					&& guildPlotOrIsland.contains(spawnLocation.toVector())) {
					player.setRespawnLocation(null);
				}
			}
		}

		if (requiresTp && !player.isOp()) {
			mTeleportingPlayers.add(player.getUniqueId());
			player.teleport(player.getWorld().getSpawnLocation());
		}
		if (requiresMessage || requiresTp) {
			player.sendMessage(getLockdownMessage(guild));
			player.playSound(player,
				Sound.ITEM_TOTEM_USE,
				SoundCategory.PLAYERS,
				0.7f,
				Constants.Note.FS4.mPitch);
		}
		if (Gui.getOpenGui(player) instanceof TeleportGuildGui teleportGuildGui) {
			teleportGuildGui.refresh();
		}
	}

	public static CompletableFuture<List<BoundingBox>> getOriginalGuildPlotAndIslands(
		World originalPlotsWorld,
		Group guild,
		boolean lockArea
	) {
		CompletableFuture<List<BoundingBox>> future = new CompletableFuture<>();
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			List<BoundingBox> result = new ArrayList<>();

			Optional<Location> optGuildPlotEntrance = getAlignedPlotEntrance(originalPlotsWorld, guild).join();
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				if (optGuildPlotEntrance.isEmpty()) {
					future.complete(result);
					return;
				}
				Location guildPlotEntrance = optGuildPlotEntrance.get();

				Vector plotDirection = VectorUtils.nearestCardinalDirection(guildPlotEntrance.getDirection());
				Vector plotSidewaysDirection = VectorUtils.swapXZAxes(plotDirection);

				// Prepare to locate the corner
				Location plotCornerA = new Location(guildPlotEntrance.getWorld(),
					guildPlotEntrance.getX(),
					10.0,
					guildPlotEntrance.getZ(),
					guildPlotEntrance.getYaw(),
					0.0f)
					.add(plotDirection.clone().multiply(5));

				// Find back side of sponge
				while (true) {
					Location testLoc = plotCornerA.clone().add(plotDirection);
					if (!testLoc.getBlock().getType().equals(Material.SPONGE)) {
						break;
					}
					plotCornerA = testLoc;
				}

				// Find back corner of sponge
				while (true) {
					Location testLoc = plotCornerA.clone().add(plotSidewaysDirection);
					if (!testLoc.getBlock().getType().equals(Material.SPONGE)) {
						break;
					}
					plotCornerA = testLoc;
				}

				// Extend a block beyond barrier in case anyone's clipping inside
				plotCornerA
					.add(plotDirection.clone().multiply(2))
					.add(plotSidewaysDirection.clone().multiply(2));

				// Include the box just outside the sponge for the door, plus a block
				Location plotCornerB = plotCornerA.clone()
					.add(plotDirection.clone().multiply(-40))
					.add(plotSidewaysDirection.clone().multiply(-34));
				BoundingBox originalGuildPlot = new BoundingBox(plotCornerA.getX(),
					10.0,
					plotCornerA.getZ(),
					plotCornerB.getX(),
					152.0,
					plotCornerB.getZ())
					// Plus 1 to include the full blocks
					.expand(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

				result.add(originalGuildPlot);

				if (lockArea) {
					// Get door bounding box
					Vector doorCornerA = plotCornerA.toVector().clone()
						.add(plotDirection.clone().multiply(-39))
						.add(plotSidewaysDirection.clone().multiply(-15));
					Vector doorCornerB = plotCornerA.toVector().clone()
						.add(plotDirection.clone().multiply(-39))
						.add(plotSidewaysDirection.clone().multiply(-19));
					BoundingBox originalGuildPlotDoor = new BoundingBox(doorCornerA.getX(),
						99.0,
						doorCornerA.getZ(),
						doorCornerB.getX(),
						103.0,
						doorCornerB.getZ())
						// Plus 1 to include the full blocks
						.expand(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

					for (int x = (int) originalGuildPlotDoor.getMinX(); x < originalGuildPlotDoor.getMaxX(); x++) {
						for (int z = (int) originalGuildPlotDoor.getMinZ(); z < originalGuildPlotDoor.getMaxZ(); z++) {
							for (int y = (int) originalGuildPlotDoor.getMinY(); y < originalGuildPlotDoor.getMaxY(); y++) {
								originalPlotsWorld.setBlockData(x, y, z, Material.BARRIER.createBlockData());
							}
						}
					}
				}

				for (Entity entity : originalPlotsWorld.getNearbyEntities(originalGuildPlot)) {
					if (!(entity instanceof Villager) || !"Lectros".equals(MessagingUtils.plainText(entity.customName()))) {
						continue;
					}
					OptionalInt optX = ScoreboardUtils.getScoreboardValue(entity, "plotx");
					OptionalInt optZ = ScoreboardUtils.getScoreboardValue(entity, "plotz");

					if (optX.isEmpty() || optZ.isEmpty()) {
						continue;
					}

					BoundingBox guildIsland = getGuildIslandBB(originalPlotsWorld, optX.getAsInt(), optZ.getAsInt());
					if (guildIsland != null) {
						result.add(guildIsland);
					}
				}

				future.complete(result);
			});
		});
		return future;
	}

	private static CompletableFuture<Optional<Location>> getAlignedPlotEntrance(World originalPlotsWorld, Group guild) {
		CompletableFuture<Optional<Location>> future = new CompletableFuture<>();

		if (!"plots".equals(ServerProperties.getShardName())) {
			future.complete(Optional.empty());
			return future;
		}

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			Optional<Location> optGuildPlotEntrance = LuckPermsIntegration.getGuildTp(originalPlotsWorld, guild).join();
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				if (optGuildPlotEntrance.isEmpty()) {
					future.complete(Optional.empty());
					return;
				}
				Location guildPlotEntrance = optGuildPlotEntrance.get();

				// Force block coordinates and direction to cardinal direction (can otherwise be off a few degrees)
				int yaw = (Math.floorMod((int) guildPlotEntrance.getYaw() + 45, 360) / 90) * 90;
				future.complete(Optional.of(new Location(originalPlotsWorld,
					guildPlotEntrance.getBlockX(),
					guildPlotEntrance.getBlockY(),
					guildPlotEntrance.getBlockZ(),
					yaw,
					0.0f)));
			});
		});

		return future;
	}

	private static @Nullable BoundingBox getGuildIslandBB(World world, int x, int z) {
		Location start = new Location(world, x, 10, z);

		if (!start.getBlock().getType().equals(Material.SPONGE)) {
			return null;
		}

		int minX = x;
		while (new Location(world, minX - 1, 10, z).getBlock().getType().equals(Material.SPONGE)) {
			minX--;
		}

		int maxX = x;
		while (new Location(world, maxX + 1, 10, z).getBlock().getType().equals(Material.SPONGE)) {
			maxX++;
		}

		int minZ = z;
		while (new Location(world, x, 10, minZ - 1).getBlock().getType().equals(Material.SPONGE)) {
			minZ--;
		}


		int maxZ = z;
		while (new Location(world, x, 10, maxZ + 1).getBlock().getType().equals(Material.SPONGE)) {
			maxZ++;
		}

		// Include the block beyond the barrier around the guild island
		return new BoundingBox(minX - 2, 10.0, minZ - 2, maxX + 3, 153.0, maxZ + 3);
	}
}

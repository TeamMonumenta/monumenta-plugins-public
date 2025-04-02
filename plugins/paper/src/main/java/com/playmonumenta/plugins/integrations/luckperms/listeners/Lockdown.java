package com.playmonumenta.plugins.integrations.luckperms.listeners;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.integrations.luckperms.GuildPlotUtils;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.integrations.luckperms.PlotsGuildBounds;
import com.playmonumenta.plugins.integrations.luckperms.TeleportGuildGui;
import com.playmonumenta.plugins.integrations.luckperms.guildgui.GuildGui;
import com.playmonumenta.plugins.plots.ShopManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.zones.Zone;
import com.playmonumenta.scriptedquests.zones.ZoneManager;
import com.playmonumenta.scriptedquests.zones.ZoneNamespace;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Pattern;
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
	public static String PLOT_ZONE_NAMESPACE_NAME = "Guild Lockdown (Buildable)";
	public static String MAILBOX_ZONE_NAMESPACE_NAME = "Guild Lockdown (Mailbox)";

	// The presence of a map indicates a guild is locked, even if the map is empty (for non-plots shards)
	private static final ConcurrentSkipListSet<Long> mLockedGuildIds = new ConcurrentSkipListSet<>();
	private static final Map<String, Map<String, PlotsGuildBounds>> mLockedDownAreas = new HashMap<>();
	private static final Set<UUID> mTeleportingPlayers = new HashSet<>();

	public Lockdown() {
		ZoneManager zoneManager = ZoneManager.getInstance();
		ZoneNamespace plotZoneNamespace = new ZoneNamespace(PLOT_ZONE_NAMESPACE_NAME);
		zoneManager.registerPluginZoneNamespace(plotZoneNamespace);
		ZoneNamespace mailboxZoneNamespace = new ZoneNamespace(MAILBOX_ZONE_NAMESPACE_NAME);
		zoneManager.registerPluginZoneNamespace(mailboxZoneNamespace);
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

				Map<String, PlotsGuildBounds> lockedGuildAreas = new HashMap<>();
				mLockedDownAreas.put(guild.getName(), lockedGuildAreas);
				Long guildPlotId = LuckPermsIntegration.getGuildPlotId(guild);
				if (guildPlotId == null) {
					MMLog.warning("[Guild Lockdown Kicker] (" + mIterations + "/" + numGuilds + ") Could not get guild's plot ID!");
				} else {
					mLockedGuildIds.add(guildPlotId);
				}
				if (originalPlotsWorld != null) {
					final int currentIter = mIterations;
					MMLog.info("[Guild Lockdown Kicker] (" + currentIter + "/" + numGuilds
						+ ") Checking guild plot locations...");
					Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
						PlotsGuildBounds plotsGuildBounds
							= getOriginalGuildPlotAndIslands(originalPlotsWorld, guild).join();
						Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
							lockedGuildAreas.put(originalPlotsWorld.getName(), plotsGuildBounds);
							refreshZones();
							MMLog.info("[Guild Lockdown Kicker] (" + currentIter + "/" + numGuilds
								+ ") Registered guild plot locations.");
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
			Set<Long> guildPlotIds = new HashSet<>();
			for (Group guild : guilds) {
				guildIds.add(guild.getName());
				Long guildPlotId = LuckPermsIntegration.getGuildPlotId(guild);
				if (guildPlotId != null) {
					guildPlotIds.add(guildPlotId);
				}
			}
			Set<String> deletedGuilds = new HashSet<>(mLockedDownAreas.keySet());
			deletedGuilds.removeAll(guildIds);
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				for (String deletedGuild : deletedGuilds) {
					mLockedDownAreas.remove(deletedGuild);
				}
			});
			ConcurrentSkipListSet<Long> deletedGuildPlotIds = new ConcurrentSkipListSet<>(mLockedGuildIds);
			deletedGuildPlotIds.removeAll(guildPlotIds);
			mLockedGuildIds.removeAll(deletedGuildPlotIds);

			// The rest of these sort out locked/unlocked state as needed
			for (Group guild : guilds) {
				Long guildPlotId = LuckPermsIntegration.getGuildPlotId(guild);
				if (guildPlotId == null) {
					// Already warned about above
					continue;
				}

				boolean wasLocked = mLockedGuildIds.contains(guildPlotId);
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
		Map<String, PlotsGuildBounds> lockedGuildAreas = new HashMap<>();
		mLockedDownAreas.put(guild.getName(), lockedGuildAreas);
		Long guildPlotId = LuckPermsIntegration.getGuildPlotId(guild);
		mLockedGuildIds.add(guildPlotId);

		World originalPlotsWorld;
		if ("plots".equals(ServerProperties.getShardName())) {
			originalPlotsWorld = Bukkit.getWorlds().get(0);

			lockOriginalGuildMarketPlot(originalPlotsWorld, guild);

			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				PlotsGuildBounds plotsGuildBounds
					= getOriginalGuildPlotAndIslands(originalPlotsWorld, guild).join();
				Bukkit.getScheduler().runTask(Plugin.getInstance(),
					() -> {
						lockedGuildAreas.put(originalPlotsWorld.getName(), plotsGuildBounds);
						refreshZones();
					});
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
		Long guildPlotId = LuckPermsIntegration.getGuildPlotId(guild);
		if (guildPlotId != null) {
			mLockedGuildIds.remove(guildPlotId);
		}
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
		refreshZones();
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
		if (!LuckPermsIntegration.isLocked(guild)) {
			return;
		}

		Map<String, PlotsGuildBounds> lockedAreas = mLockedDownAreas.get(guild.getName());

		boolean requiresMessage = hasNotSeenMessage && player.hasPermission("group." + guild.getName());
		boolean requiresTp = false;
		Location playerLocation = player.getLocation();
		Location spawnLocation = player.getRespawnLocation();

		Set<String> checkedWorldNames = new HashSet<>();
		checkedWorldNames.add(playerLocation.getWorld().getName());
		if (spawnLocation != null) {
			checkedWorldNames.add(spawnLocation.getWorld().getName());
		}

		// On the plots shard
		if (lockedAreas != null) {
			for (Map.Entry<String, PlotsGuildBounds> lockedAreaEntry : lockedAreas.entrySet()) {
				String worldName = lockedAreaEntry.getKey();
				if (!checkedWorldNames.contains(worldName)) {
					continue;
				}

				for (BoundingBox guildPlotOrIsland : lockedAreaEntry.getValue().allBbs()) {
					if (worldName.equals(playerLocation.getWorld().getName())
						&& guildPlotOrIsland.overlaps(player.getBoundingBox())) {
						requiresTp = true;
					}

					if (spawnLocation != null
						&& worldName.equals(spawnLocation.getWorld().getName())
						&& guildPlotOrIsland.contains(spawnLocation.toVector())) {
						player.setRespawnLocation(null);
					}
				}
			}
		}

		// On the guildplots shard
		Long guildPlotId = LuckPermsIntegration.getGuildPlotId(guild);
		if (
			ServerProperties.getShardName().startsWith(GuildPlotUtils.SHARD_NAME)
			&& guildPlotId != null
		) {
			String guildPlotWorldName = GuildPlotUtils.guildPlotName(guildPlotId);
			if (spawnLocation != null && guildPlotWorldName.equals(spawnLocation.getWorld().getName())) {
				player.setRespawnLocation(null);
			}
			requiresTp |= playerLocation.getWorld().getName().equals(guildPlotWorldName);
		}

		if (requiresTp) {
			if (player.hasPermission(GuildGui.MOD_GUI_PERMISSION)) {
				player.sendMessage(Component.text(
					"Your operator status allows you to stay in this locked guild plot.",
					NamedTextColor.RED
				));
			} else {
				mTeleportingPlayers.add(player.getUniqueId());
				if (ServerProperties.getShardName().startsWith(GuildPlotUtils.SHARD_NAME)) {
					GuildPlotUtils.sendGuildPlotFallbackWorld(player, false);
				} else {
					player.teleport(player.getWorld().getSpawnLocation());
				}
			}
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

	public static CompletableFuture<@Nullable PlotsGuildBounds> getOriginalGuildPlotAndIslands(
		World originalPlotsWorld,
		Group guild
	) {
		CompletableFuture<@Nullable PlotsGuildBounds> future = new CompletableFuture<>();
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			PlotsGuildBounds plotsGuildBounds = PlotsGuildBounds.getGuildBounds(originalPlotsWorld, guild).join();
			if (plotsGuildBounds == null) {
				future.complete(null);
				return;
			}

			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				// Get door bounding box
				BoundingBox doorBb = plotsGuildBounds.doorBb();

				for (int x = (int) doorBb.getMinX(); x < doorBb.getMaxX(); x++) {
					for (int z = (int) doorBb.getMinZ(); z < doorBb.getMaxZ(); z++) {
						for (int y = (int) doorBb.getMinY(); y < doorBb.getMaxY(); y++) {
							originalPlotsWorld.setBlockData(x, y, z, Material.BARRIER.createBlockData());
						}
					}
				}
				future.complete(plotsGuildBounds);
			});
		});
		return future;
	}

	private static void refreshZones() {
		if (!"plots".equals(ServerProperties.getShardName())) {
			return;
		}

		ZoneManager zoneManager = ZoneManager.getInstance();
		ZoneNamespace plotZoneNamespace = new ZoneNamespace(PLOT_ZONE_NAMESPACE_NAME);
		ZoneNamespace mailboxZoneNamespace = new ZoneNamespace(MAILBOX_ZONE_NAMESPACE_NAME);

		int lockdownBoxNum = 1;
		for (Map<String, PlotsGuildBounds> guildLockdownEntry : mLockedDownAreas.values()) {
			for (Map.Entry<String, PlotsGuildBounds> worldEntries : guildLockdownEntry.entrySet()) {
				String worldName = worldEntries.getKey();
				String escapedWorldName = Pattern.quote(worldName);
				PlotsGuildBounds plotsGuildBounds = worldEntries.getValue();

				for (BoundingBox bb : plotsGuildBounds.allBuildable()) {
					Zone zone = new Zone(
						plotZoneNamespace,
						escapedWorldName,
						bb.getMin(),
						// Zones add this back in automatically
						bb.getMax().subtract(new Vector(1.0, 1.0, 1.0)),
						"Buildable Lockdown Bounding Box " + lockdownBoxNum,
						Set.of("On Lockdown")
					);
					plotZoneNamespace.addZone(zone);
					lockdownBoxNum++;
				}

				for (BoundingBox bb : plotsGuildBounds.mailboxBbs()) {
					Zone zone = new Zone(
						mailboxZoneNamespace,
						escapedWorldName,
						bb.getMin(),
						// Zones add this back in automatically
						bb.getMax().subtract(new Vector(1.0, 1.0, 1.0)),
						"Mailbox Lockdown Bounding Box " + lockdownBoxNum,
						Set.of("On Lockdown")
					);
					mailboxZoneNamespace.addZone(zone);
					lockdownBoxNum++;
				}
			}
		}

		zoneManager.replacePluginZoneNamespace(plotZoneNamespace);
		zoneManager.replacePluginZoneNamespace(mailboxZoneNamespace);
	}
}

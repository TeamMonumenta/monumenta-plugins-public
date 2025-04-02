package com.playmonumenta.plugins.integrations.luckperms;

import com.bergerkiller.bukkit.common.chunk.ForcedChunk;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.listeners.EntityListener;
import com.playmonumenta.plugins.listeners.WorldListener;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.shardhealth.ShardHealthManager;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import com.playmonumenta.structures.StructuresAPI;
import com.playmonumenta.worlds.paper.MonumentaWorldManagementAPI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentSkipListSet;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable.AttachedFace;
import org.bukkit.block.data.Hangable;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.AmethystCluster;
import org.bukkit.block.data.type.Bell;
import org.bukkit.block.data.type.CoralWallFan;
import org.bukkit.block.data.type.GlowLichen;
import org.bukkit.block.data.type.Ladder;
import org.bukkit.block.data.type.PointedDripstone;
import org.bukkit.block.data.type.SculkVein;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.data.type.TripwireHook;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Villager;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PlotsGuildBounds {
	private static final EnumSet<Material> CEILING_REQUIRED_MATS = EnumSet.of(
		Material.CAVE_VINES,
		Material.SPORE_BLOSSOM,
		Material.WEEPING_VINES
	);

	private enum GuildPlotOrigin {
		SOUTH(-33, 57, 139),
		WEST(-125, 57, 56),
		NORTH(23, 57, -105),
		EAST(132, 57, -2),
		;

		private final int mX;
		private final int mY;
		private final int mZ;

		GuildPlotOrigin(int x, int y, int z) {
			mX = x;
			mY = y;
			mZ = z;
		}

		public static GuildPlotOrigin byFacing(int facingAngle) {
			return switch (facingAngle) {
				case 90 -> WEST;
				case 180 -> NORTH;
				case 270 -> EAST;
				default -> SOUTH;
			};
		}

		public Vector vector() {
			return new Vector(mX, mY, mZ);
		}
	}

	public final String mGuildName;
	private final long mPlotNumber;
	private final int mFacingAngle;
	private final Vector mOrigin;
	private final Vector mSpawnPoint;
	private final BoundingBox mDoorBb;
	private final List<BoundingBox> mPlotBbs;
	private final List<BoundingBox> mMailboxBbs;
	private final List<BoundingBox> mIslandBbs;

	private PlotsGuildBounds(
		String guildName,
		long plotNumber,
		int facingAngle,
		Vector origin,
		Vector spawnPoint,
		BoundingBox doorBb,
		List<BoundingBox> plotBbs,
		List<BoundingBox> mailboxBbs,
		List<BoundingBox> islandBbs
	) {
		mGuildName = guildName;
		mPlotNumber = plotNumber;
		mFacingAngle = facingAngle;
		mOrigin = origin;
		mSpawnPoint = spawnPoint;
		mDoorBb = doorBb;
		mPlotBbs = plotBbs;
		mMailboxBbs = mailboxBbs;
		mIslandBbs = islandBbs;
	}

	public static PlotsGuildBounds fromJson(String guildName, JsonElement rootElement) {
		if (!(rootElement instanceof JsonObject root)) {
			throw new RuntimeException("Unexpected json type");
		}

		long plotNumber = root.get("plot_number").getAsLong();
		int facingAngle = root.get("facing").getAsInt();
		Vector origin = vectorFromJson(root.get("origin"));
		Vector spawnPoint = vectorFromJson(root.get("spawn_point"));
		BoundingBox doorBb = bbFromJson(root.get("door"));
		List<BoundingBox> plotBbs = bbArrayFromJson(root.get("plot"));
		List<BoundingBox> mailboxBbs = bbArrayFromJson(root.get("mailbox"));
		List<BoundingBox> islandBbs = bbArrayFromJson(root.get("island"));

		return new PlotsGuildBounds(
			guildName,
			plotNumber,
			facingAngle,
			origin,
			spawnPoint,
			doorBb,
			plotBbs,
			mailboxBbs,
			islandBbs
		);
	}

	public static CompletableFuture<@Nullable PlotsGuildBounds> getGuildBounds(
		World originalPlotsWorld,
		Group guild
	) {
		return getGuildBounds(Audience.empty(), originalPlotsWorld, guild);
	}

	public static CompletableFuture<@Nullable PlotsGuildBounds> getGuildBounds(
		Audience audience,
		World originalPlotsWorld,
		Group guild
	) {
		CompletableFuture<@Nullable PlotsGuildBounds> future = new CompletableFuture<>();
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			ShardHealthManager.awaitShardHealth(audience).join();
			Optional<Location> optGuildPlotEntrance = getAlignedPlotEntrance(originalPlotsWorld, guild).join();

			Long plotNumber = LuckPermsIntegration.getGuildPlotId(guild);
			String guildName = LuckPermsIntegration.getNonNullGuildName(guild);

			if (plotNumber == null) {
				future.complete(null);
				return;
			}

			if (optGuildPlotEntrance.isEmpty()) {
				future.complete(null);
				return;
			}
			Location guildPlotEntrance = optGuildPlotEntrance.get();

			Vector plotDirection = VectorUtils.nearestCardinalDirection(guildPlotEntrance.getDirection());
			// Rotate 90 degrees, without the imprecision of using sin/cos using radians as an intermediate
			Vector plotSidewaysDirection = new Vector(-plotDirection.getBlockZ(), 0, plotDirection.getBlockX());
			guildPlotEntrance.setDirection(plotDirection);
			int facingAngle = (int) guildPlotEntrance.getYaw();

			CompletableFuture<Vector> originFuture = new CompletableFuture<>();
			CompletableFuture<BoundingBox> originalGuildPlotDoorFuture = new CompletableFuture<>();
			CompletableFuture<List<BoundingBox>> plotBbsFuture = new CompletableFuture<>();
			CompletableFuture<List<BoundingBox>> mailboxBbsFuture = new CompletableFuture<>();

			CompletableFuture<Void> createdGuildIslandFutures = new CompletableFuture<>();
			List<CompletableFuture<List<Location>>> guildIslandLocsFutures = new ArrayList<>();

			ShardHealthManager.awaitShardHealth(audience).join();
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				List<BoundingBox> plotBbs = new ArrayList<>();
				List<BoundingBox> mailboxBbs = new ArrayList<>();

				// Prepare to locate the corner
				Location plotCornerA = new Location(guildPlotEntrance.getWorld(),
					guildPlotEntrance.getBlockX(),
					10.0,
					guildPlotEntrance.getBlockZ(),
					facingAngle,
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

				// Don't include the box just before the door
				Location plotCornerB = plotCornerA.clone()
					.add(plotDirection.clone().multiply(-34))
					.add(plotSidewaysDirection.clone().multiply(-30));
				BoundingBox originalGuildPlot = new BoundingBox(
					plotCornerA.getX(),
					68.0,
					plotCornerA.getZ(),
					plotCornerB.getX(),
					148.0,
					plotCornerB.getZ()
				)
					// Plus 1 to include the full blocks
					.expand(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
				plotBbs.add(originalGuildPlot);

				Vector origin = plotCornerA.clone().toVector()
					.add(plotDirection.clone().multiply(-35))
					.add(plotSidewaysDirection.clone().multiply(-15));
				origin = new Vector((long) origin.getX(), 99L, (long) origin.getZ());
				originFuture.complete(origin);

				// Get doorway in separate pieces
				Location entrywayCornerA;
				Location entrywayCornerB;
				BoundingBox doorPart;

				// Against door, one piece
				entrywayCornerA = plotCornerA.clone()
					.add(plotDirection.clone().multiply(-36))
					.add(plotSidewaysDirection.clone().multiply(-16));
				entrywayCornerB = plotCornerA.clone()
					.add(plotDirection.clone().multiply(-36))
					.add(plotSidewaysDirection.clone().multiply(-14));
				doorPart = new BoundingBox(
					entrywayCornerA.getX(),
					99.0,
					entrywayCornerA.getZ(),
					entrywayCornerB.getX(),
					102.0,
					entrywayCornerB.getZ()
				)
					// Plus 1 to include the full blocks
					.expand(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
				plotBbs.add(doorPart);

				// Against plot, upper
				entrywayCornerA = plotCornerA.clone()
					.add(plotDirection.clone().multiply(-35))
					.add(plotSidewaysDirection.clone().multiply(-16));
				entrywayCornerB = plotCornerA.clone()
					.add(plotDirection.clone().multiply(-35))
					.add(plotSidewaysDirection.clone().multiply(-14));
				doorPart = new BoundingBox(
					entrywayCornerA.getX(),
					102.0,
					entrywayCornerA.getZ(),
					entrywayCornerB.getX(),
					103.0,
					entrywayCornerB.getZ()
				)
					// Plus 1 to include the full blocks
					.expand(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
				plotBbs.add(doorPart);

				// Against plot, lower
				entrywayCornerA = plotCornerA.clone()
					.add(plotDirection.clone().multiply(-35))
					.add(plotSidewaysDirection.clone().multiply(-17));
				entrywayCornerB = plotCornerA.clone()
					.add(plotDirection.clone().multiply(-35))
					.add(plotSidewaysDirection.clone().multiply(-13));
				doorPart = new BoundingBox(
					entrywayCornerA.getX(),
					99.0,
					entrywayCornerA.getZ(),
					entrywayCornerB.getX(),
					101.0,
					entrywayCornerB.getZ()
				)
					// Plus 1 to include the full blocks
					.expand(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
				plotBbs.add(doorPart);

				// Floor blocks
				entrywayCornerA = plotCornerA.clone()
					.add(plotDirection.clone().multiply(-35))
					.add(plotSidewaysDirection.clone().multiply(-16));
				entrywayCornerB = plotCornerA.clone()
					.add(plotDirection.clone().multiply(-37))
					.add(plotSidewaysDirection.clone().multiply(-14));
				doorPart = new BoundingBox(
					entrywayCornerA.getX(),
					98.0,
					entrywayCornerA.getZ(),
					entrywayCornerB.getX(),
					98.0,
					entrywayCornerB.getZ()
				)
					// Plus 1 to include the full blocks
					.expand(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
				plotBbs.add(doorPart);

				plotBbsFuture.complete(plotBbs);

				// Get door bounds
				Vector doorCornerA = plotCornerA.toVector().clone()
					.add(plotDirection.clone().multiply(-37))
					.add(plotSidewaysDirection.clone().multiply(-13));
				Vector doorCornerB = plotCornerA.toVector().clone()
					.add(plotDirection.clone().multiply(-37))
					.add(plotSidewaysDirection.clone().multiply(-17));
				BoundingBox originalGuildPlotDoor = new BoundingBox(
					doorCornerA.getX(),
					99.0,
					doorCornerA.getZ(),
					doorCornerB.getX(),
					103.0,
					doorCornerB.getZ()
				)
					// Plus 1 to include the full blocks
					.expand(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
				originalGuildPlotDoorFuture.complete(originalGuildPlotDoor);

				// Mailbox bounds
				Location mailboxCornerA = plotCornerA.clone()
					.add(plotDirection.clone().multiply(-35))
					.add(plotSidewaysDirection.clone().multiply(-10));
				Location mailboxCornerB = plotCornerA.clone()
					.add(plotDirection.clone().multiply(-36))
					.add(plotSidewaysDirection.clone().multiply(-11));
				mailboxBbs.add(new BoundingBox(
					mailboxCornerA.getX(),
					96.0,
					mailboxCornerA.getZ(),
					mailboxCornerB.getX(),
					99.0,
					mailboxCornerB.getZ()
				).expand(0.0, 0.0, 0.0, 1.0, 1.0, 1.0));

				mailboxBbsFuture.complete(mailboxBbs);

				ConcurrentSkipListSet<UUID> seen = new ConcurrentSkipListSet<>();
				for (BoundingBox bb : plotBbs) {
					BoundingBox plotBb = bb.clone()
						.expand(0.1, 0.1, 0.1, 1.1, 1.1, 1.1);

					int minCx = Math.floorDiv(plotBb.getMin().getBlockX(), 16);
					int minCz = Math.floorDiv(plotBb.getMin().getBlockZ(), 16);
					int maxCx = Math.floorDiv(plotBb.getMax().getBlockX(), 16);
					int maxCz = Math.floorDiv(plotBb.getMax().getBlockZ(), 16);
					for (int cx = minCx; cx <= maxCx; cx++) {
						int finalCx = cx;
						for (int cz = minCz; cz <= maxCz; cz++) {
							int finalCz = cz;
							CompletableFuture<List<Location>> guildIslandLocsFuture = new CompletableFuture<>();
							guildIslandLocsFutures.add(guildIslandLocsFuture);

							Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
								ShardHealthManager.awaitShardHealth(audience).join();
								try (ForcedChunk forcedChunk = ForcedChunk.load(originalPlotsWorld, finalCx, finalCz)) {
									CompletableFuture<Void> doneWithChunkFuture = new CompletableFuture<>();
									Chunk chunk = forcedChunk.getChunkAsync().join();

									ShardHealthManager.awaitShardHealth(audience).join();
									Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
										List<Location> guildIslandLocs = new ArrayList<>();

										for (Entity entity : chunk.getEntities()) {
											if (!plotBb.contains(entity.getLocation().toVector())) {
												continue;
											}

											if (!(entity instanceof Villager)) {
												continue;
											}

											Component customName = entity.customName();
											if (customName == null) {
												customName = Component.text("null", NamedTextColor.RED);
											}
											if (!"Lectros".equals(MessagingUtils.plainText(customName))) {
												continue;
											}
											OptionalInt optX = ScoreboardUtils.getScoreboardValue(entity, "plotx");
											OptionalInt optZ = ScoreboardUtils.getScoreboardValue(entity, "plotz");

											if (optX.isEmpty() || optZ.isEmpty()) {
												LuckPermsIntegration.onlineDevOpAudience().sendMessage(Component.text(
													"Found Lectros with no teleport location at "
														+ entity.getLocation().toVector(),
													NamedTextColor.RED
												));
												continue;
											}

											if (!seen.add(entity.getUniqueId())) {
												continue;
											}

											guildIslandLocs.add(
												new Location(originalPlotsWorld, optX.getAsInt(), 10, optZ.getAsInt()));
										}
										guildIslandLocsFuture.complete(guildIslandLocs);

										doneWithChunkFuture.complete(null);
									});
									doneWithChunkFuture.join();
								}
							});
						}
					}
				}
				createdGuildIslandFutures.complete(null);
			});

			createdGuildIslandFutures.join();

			List<Location> guildIslandLocs = new ArrayList<>();
			for (CompletableFuture<List<Location>> guildIslandLocsFuture : guildIslandLocsFutures) {
				guildIslandLocs.addAll(guildIslandLocsFuture.join());
			}
			Vector origin = originFuture.join();
			BoundingBox originalGuildPlotDoor = originalGuildPlotDoorFuture.join();
			List<BoundingBox> plotBbs = plotBbsFuture.join();
			List<BoundingBox> mailboxBbs = mailboxBbsFuture.join();

			ShardHealthManager.awaitShardHealth(audience).join();
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				List<BoundingBox> islandBbs = new ArrayList<>();

				for (Location guildIslandLoc : guildIslandLocs) {
					int x = guildIslandLoc.getBlockX();
					int z = guildIslandLoc.getBlockZ();
					BoundingBox guildIsland = getGuildIslandBB(originalPlotsWorld, x, z);
					if (guildIsland == null) {
						LuckPermsIntegration.onlineDevOpAudience().sendMessage(Component.text(
							"Failed to find guild island at " + x + ", " + z + ": did a previous message appear?",
							NamedTextColor.RED
						));
					} else {
						islandBbs.add(guildIsland);
					}
				}

				future.complete(new PlotsGuildBounds(
					guildName,
					plotNumber,
					facingAngle,
					origin,
					guildPlotEntrance.toVector(),
					originalGuildPlotDoor,
					plotBbs,
					mailboxBbs,
					islandBbs
				));
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

		Material mat = start.getBlock().getType();
		if (!mat.equals(Material.SPONGE)) {
			LuckPermsIntegration.onlineDevOpAudience().sendMessage(Component.text(
				"Failed to find guild island at " + x + ", " + z + ": Found " + mat.getKey() + " instead",
				NamedTextColor.RED
			));
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

		return new BoundingBox(minX, 13.0, minZ, maxX, 151.0, maxZ)
			// Plus 1 to include the full blocks
			.expand(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
	}

	public @Nullable Long plotNumber() {
		return mPlotNumber;
	}

	public int facingAngle() {
		return mFacingAngle;
	}

	public Vector origin() {
		return mOrigin;
	}

	public Vector spawnPoint() {
		return mSpawnPoint;
	}

	public BoundingBox doorBb() {
		return mDoorBb;
	}

	public List<BoundingBox> plotBbs() {
		return new ArrayList<>(mPlotBbs);
	}

	public List<BoundingBox> mailboxBbs() {
		return new ArrayList<>(mMailboxBbs);
	}

	public List<BoundingBox> islandBbs() {
		return new ArrayList<>(mIslandBbs);
	}

	public List<BoundingBox> allBbs() {
		List<BoundingBox> result = new ArrayList<>(mPlotBbs);
		result.addAll(mMailboxBbs);
		result.addAll(mIslandBbs);
		return result;
	}

	public List<BoundingBox> allBuildable() {
		List<BoundingBox> result = new ArrayList<>(mPlotBbs);
		result.addAll(mIslandBbs);
		return result;
	}

	private CompletableFuture<Location> getSyncLocation(World world, Vector vector) {
		CompletableFuture<Location> future = new CompletableFuture<>();
		Bukkit.getScheduler().runTask(Plugin.getInstance(),
			() -> future.complete(new Location(world, vector.getX(), vector.getY(), vector.getZ(), mFacingAngle, 0)));
		return future;
	}

	// /savestructure "guildplots_export/1/plot/0" ~1 ~2 ~3 ~4 ~5 ~6
	public CompletableFuture<Void> saveStructures(Audience audience, World world) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			String prefix = "guildplots_export/" + mPlotNumber + "/";

			// guildplots_export/1/plot/0
			for (int i = 0; i < mPlotBbs.size(); i++) {
				List<ForcedChunk> forcedChunks = new ArrayList<>();
				BoundingBox bb = mPlotBbs.get(i);
				Location min = getSyncLocation(world, bb.getMin()).join();
				Location max = getSyncLocation(world, bb.getMax()).join().subtract(new Vector(1, 1, 1));
				String path = prefix + "plot/" + i;

				int minCx = Math.floorDiv(min.getBlockX(), 16);
				int maxCx = Math.floorDiv(max.getBlockX(), 16);
				int minCz = Math.floorDiv(min.getBlockZ(), 16);
				int maxCz = Math.floorDiv(max.getBlockZ(), 16);
				for (int cx = minCx; cx <= maxCx; cx++) {
					for (int cz = minCz; cz <= maxCz; cz++) {
						forcedChunks.add(ForcedChunk.load(world, cx, cz, 3));
					}
				}

				for (ForcedChunk forcedChunk : forcedChunks) {
					forcedChunk.getChunkAsync().join();
				}
				ShardHealthManager.awaitShardHealth(audience).join();
				StructuresAPI.copyAreaAndSaveStructure(path, min, max).join();
				for (ForcedChunk forcedChunk : forcedChunks) {
					forcedChunk.close();
				}
			}

			// guildplots_export/1/island/0
			for (int i = 0; i < mIslandBbs.size(); i++) {
				List<ForcedChunk> forcedChunks = new ArrayList<>();
				BoundingBox bb = mIslandBbs.get(i);
				Location min = getSyncLocation(world, bb.getMin()).join();
				Location max = getSyncLocation(world, bb.getMax()).join().subtract(new Vector(1, 1, 1));
				String path = prefix + "island/" + i;

				int minCx = Math.floorDiv(min.getBlockX(), 16);
				int maxCx = Math.floorDiv(max.getBlockX(), 16);
				int minCz = Math.floorDiv(min.getBlockZ(), 16);
				int maxCz = Math.floorDiv(max.getBlockZ(), 16);
				for (int cx = minCx; cx <= maxCx; cx++) {
					for (int cz = minCz; cz <= maxCz; cz++) {
						forcedChunks.add(ForcedChunk.load(world, cx, cz, 3));
					}
				}

				for (ForcedChunk forcedChunk : forcedChunks) {
					forcedChunk.getChunkAsync().join();
				}
				ShardHealthManager.awaitShardHealth(audience).join();
				StructuresAPI.copyAreaAndSaveStructure(path, min, max).join();
				for (ForcedChunk forcedChunk : forcedChunks) {
					forcedChunk.close();
				}
			}

			future.complete(null);
		});
		return future;
	}

	public CompletableFuture<Void> loadStructures(Audience audience, Player player) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			// Get the delta coordinates for both the plot (the island has a constant location)
			Vector deltaPlotPos = GuildPlotOrigin.byFacing(mFacingAngle).vector().subtract(mOrigin);
			BoundingBox islandBb = new BoundingBox(-87, 13, -87, 87, 152, 87);
			Vector deltaIslandPos;
			if (!mIslandBbs.isEmpty()) {
				deltaIslandPos = islandBb.getMin().clone().subtract(mIslandBbs.get(0).getMin());
			} else {
				deltaIslandPos = null;
			}

			// Get the guild plot world
			ShardHealthManager.awaitShardHealth(audience).join();
			String expectedWorldName = GuildPlotUtils.guildPlotName(mPlotNumber);
			World world;
			CompletableFuture<World> loadWorldFuture = new CompletableFuture<>();
			BukkitRunnable loadWorldRunnable = new BukkitRunnable() {
				boolean mFirstRun = true;
				int mTicksToTimeout = 20 * 60;

				@Override
				public void run() {
					if (mFirstRun) {
						mFirstRun = false;

						// Actually send the player to the guild plot world
						ScoreboardUtils.setScoreboardValue(player, "Guild", (int) mPlotNumber);
						try {
							MonumentaWorldManagementAPI.sortWorld(player);
						} catch (Exception ex) {
							audience.sendMessage(Component.text("Unable to start world transfer?", NamedTextColor.RED));
							future.completeExceptionally(ex);
							this.cancel();
							return;
						}
					}

					World playerWorld = player.getWorld();
					if (expectedWorldName.equals(playerWorld.getName())) {
						Location spawnLocation = new Location(playerWorld, 0, 0, 0, mFacingAngle, 0);
						spawnLocation.add(mSpawnPoint);
						spawnLocation.add(deltaPlotPos);
						player.teleport(spawnLocation);

						playerWorld.setSpawnLocation(spawnLocation);
						playerWorld.setDifficulty(Difficulty.NORMAL);
						loadWorldFuture.complete(playerWorld);
						this.cancel();
						return;
					}

					mTicksToTimeout--;
					if (mTicksToTimeout <= 0) {
						loadWorldFuture.completeExceptionally(new Exception("Timed out trying to load world!"));
					}
				}
			};
			loadWorldRunnable.runTaskTimer(Plugin.getInstance(), 0L, 1L);

			// Wait for the world transfer to wrap up
			ShardHealthManager.awaitShardHealth(audience).join();
			try {
				world = loadWorldFuture.join();
			} catch (CompletionException ex) {
				audience.sendMessage(Component.text("Unable to transfer worlds?", NamedTextColor.RED));
				future.completeExceptionally(ex);
				return;
			}

			// Start pasting in the plot schematics
			String prefix = "guildplots_export/" + mPlotNumber + "/";

			// Get list of all bounding boxes that had things pasted to them as we go
			List<BoundingBox> pasteBbs = new ArrayList<>();

			EntityListener.mProtectHangingWorlds.add(expectedWorldName);
			// guildplots_export/1/plot/0
			for (int i = 0; i < mPlotBbs.size(); i++) {
				BoundingBox bb = mPlotBbs.get(i).clone();
				bb.shift(deltaPlotPos);
				Location guildPlotMin = getSyncLocation(world, bb.getMin()).join();

				pasteBbs.add(bb);
				String path = prefix + "plot/" + i;
				WorldListener.startLoadingScoresInBounds(expectedWorldName, bb);
				ShardHealthManager.awaitShardHealth(audience).join();
				StructuresAPI.loadAndPasteStructure(path, guildPlotMin, true, false).join();
				WorldListener.stopLoadingScoresInBounds(expectedWorldName, bb);
			}

			// Adjust Lectros coordinates if needed
			if (deltaIslandPos != null) {
				Location worldSpawn = world.getSpawnLocation();
				try (ForcedChunk forcedChunk = ForcedChunk.load(world,
					Math.floorDiv(worldSpawn.getBlockX(), 16),
					Math.floorDiv(worldSpawn.getBlockZ(), 16),
					6
				)) {
					forcedChunk.getChunkAsync().join();
					CompletableFuture<Void> lectrosSearchFuture = new CompletableFuture<>();
					Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
						for (Entity entity : EntityUtils.getNearbyMobs(
							worldSpawn, 40.0, 200.0, 40.0,
							(LivingEntity entity) -> entity instanceof Villager)
						) {
							Component customName = entity.customName();
							if (customName == null) {
								customName = Component.text("null", NamedTextColor.RED);
							}
							if (!"Lectros".equals(MessagingUtils.plainText(customName))) {
								continue;
							}

							ScoreboardUtils.addScore(entity, "plotx", deltaIslandPos.getBlockX());
							ScoreboardUtils.addScore(entity, "ploty", deltaIslandPos.getBlockY());
							ScoreboardUtils.addScore(entity, "plotz", deltaIslandPos.getBlockZ());
						}
						lectrosSearchFuture.complete(null);
					});
					lectrosSearchFuture.join();
				}
			}

			// guildplots_export/1/island/0
			for (int i = 0; i < mIslandBbs.size(); i++) {
				Location min = getSyncLocation(world, islandBb.getMin().clone()).join();

				pasteBbs.add(islandBb);
				String path = prefix + "island/" + i;
				WorldListener.startLoadingScoresInBounds(expectedWorldName, islandBb);
				ShardHealthManager.awaitShardHealth(audience).join();
				StructuresAPI.loadAndPasteStructure(path, min, true, false).join();
				WorldListener.stopLoadingScoresInBounds(expectedWorldName, islandBb);
			}

			// Now search for any blocks/entities that need to attach to a block and secure them
			for (BoundingBox bb : pasteBbs) {
				Vector min = bb.getMin();
				Vector max = bb.getMax().clone().subtract(new Vector(1, 1, 1));

				checkForInsecureThings(
					audience,
					world,
					new Vector(min.getBlockX(), max.getBlockY(), min.getBlockZ()),
					new Vector(max.getBlockX(), max.getBlockY(), max.getBlockZ()),
					new Vector(0, 1, 0),
					BlockFace.UP,
					BlockFace.DOWN,
					pasteBbs
				).join();
				checkForInsecureThings(
					audience,
					world,
					new Vector(min.getBlockX(), min.getBlockY(), min.getBlockZ()),
					new Vector(min.getBlockX(), max.getBlockY(), max.getBlockZ()),
					new Vector(-1, 0, 0),
					BlockFace.WEST,
					BlockFace.EAST,
					pasteBbs
				).join();
				checkForInsecureThings(
					audience,
					world,
					new Vector(max.getBlockX(), min.getBlockY(), min.getBlockZ()),
					new Vector(max.getBlockX(), max.getBlockY(), max.getBlockZ()),
					new Vector(1, 0, 0),
					BlockFace.EAST,
					BlockFace.WEST,
					pasteBbs
				).join();
				checkForInsecureThings(
					audience,
					world,
					new Vector(min.getBlockX(), min.getBlockY(), min.getBlockZ()),
					new Vector(max.getBlockX(), max.getBlockY(), min.getBlockZ()),
					new Vector(0, 0, -1),
					BlockFace.NORTH,
					BlockFace.SOUTH,
					pasteBbs
				).join();
				checkForInsecureThings(
					audience,
					world,
					new Vector(min.getBlockX(), min.getBlockY(), max.getBlockZ()),
					new Vector(max.getBlockX(), max.getBlockY(), max.getBlockZ()),
					new Vector(0, 0, 1),
					BlockFace.SOUTH,
					BlockFace.NORTH,
					pasteBbs
				).join();
			}

			EntityListener.mProtectHangingWorlds.remove(expectedWorldName);

			future.complete(null);
		});
		return future;
	}

	// For securing blocks/entities that require a block
	private CompletableFuture<Void> checkForInsecureThings(
		Audience audience,
		World world,
		Vector min,
		Vector max,
		Vector sideToSecure,
		BlockFace faceToSecure,
		BlockFace oppositeFace,
		List<BoundingBox> relevantBoundingBoxes
	) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			int minX = min.getBlockX();
			int minY = min.getBlockY();
			int minZ = min.getBlockZ();
			int maxX = max.getBlockX();
			int maxY = max.getBlockY();
			int maxZ = max.getBlockZ();

			int minCx = Math.floorDiv(minX, 16);
			int minCz = Math.floorDiv(minZ, 16);
			int maxCx = Math.floorDiv(maxX, 16);
			int maxCz = Math.floorDiv(maxZ, 16);

			for (int cx = maxCx; cx >= minCx; cx--) {
				for (int cz = maxCz; cz >= minCz; cz--) {
					ShardHealthManager.awaitShardHealth(audience).join();
					secureChunkIfNeeded(
						world,
						cx,
						cz,
						new Vector(Math.max(minX, 16 * cx), minY, Math.max(minZ, 16 * cz)),
						new Vector(Math.min(maxX, 16 * cx + 15), maxY, Math.min(maxZ, 16 * cz + 15)),
						sideToSecure,
						faceToSecure,
						oppositeFace,
						relevantBoundingBoxes
					).join();
				}
			}

			future.complete(null);
		});

		return future;
	}

	private CompletableFuture<Void> secureChunkIfNeeded(
		World world,
		int cx,
		int cz,
		Vector min,
		Vector max,
		Vector sideToSecure,
		BlockFace faceToSecure,
		BlockFace oppositeFace,
		List<BoundingBox> relevantBoundingBoxes
	) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		int minX = min.getBlockX();
		int minY = min.getBlockY();
		int minZ = min.getBlockZ();
		int maxX = max.getBlockX();
		int maxY = max.getBlockY();
		int maxZ = max.getBlockZ();

		// We need the chunk forceloaded, but it lacks the methods to interact with it directly how we want to.
		// Instead, we carefully stay within the chunk coordinates.
		try (ForcedChunk forcedChunk = ForcedChunk.load(world, cx, cz, 1)) {
			forcedChunk.getChunkAsync().join();
			CompletableFuture<Void> doneWithChunkFuture = new CompletableFuture<>();

			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				for (int x = maxX; x >= minX; x--) {
					for (int z = maxZ; z >= minZ; z--) {
						for (int y = maxY; y >= minY; y--) {
							Location testLoc = new Location(world, x, y, z);
							Location secureLoc = testLoc.clone().add(sideToSecure);
							if (needToSecure(testLoc, secureLoc, faceToSecure, oppositeFace, relevantBoundingBoxes)) {
								secure(secureLoc);
							}
						}
					}
				}

				future.complete(null);
				doneWithChunkFuture.complete(null);
			});

			doneWithChunkFuture.join();
		}

		return future;
	}

	private boolean needToSecure(
		Location testLoc,
		Location secureLoc,
		BlockFace faceToSecure,
		BlockFace oppositeFace,
		List<BoundingBox> relevantBoundingBlocks
	) {
		// Inside the plot/island, not our problem
		if (inAny(secureLoc.clone().toCenterLocation().toVector(), relevantBoundingBlocks)) {
			return false;
		}

		Block block = testLoc.getBlock();
		Material blockMat = block.getType();
		String blockMatKeyStr = blockMat.key().toString();
		BlockData blockData = block.getBlockData();

		Block secureBlock = secureLoc.getBlock();
		Material secureBlockMat = secureBlock.getType();
		// Already secured (probably checked multiple times near plot door)
		if (
			Material.GLASS.equals(secureBlockMat)
				|| secureBlockMat.isOccluding()
		) {
			return false;
		}

		// Don't flood places if possible (water doesn't match on both sides)
		if (
			BlockUtils.isWaterSource(secureBlock)
				^ BlockUtils.isWaterSource(block)
		) {
			return true;
		}

		// Protect things that only hang from the ceiling
		if (BlockFace.UP.equals(faceToSecure)) {
			if (
				(
					blockData instanceof Hangable hangable
						&& hangable.isHanging()
				)
					|| CEILING_REQUIRED_MATS.contains(blockMat)
			) {
				return true;
			}

			if (blockData instanceof PointedDripstone pointedDripstone) {
				BlockFace blockFace = pointedDripstone.getVerticalDirection();
				if (blockFace.equals(oppositeFace)) {
					return true;
				}
			}
		}

		// Protect other blocks that are stuck onto a surface that might not be there anymore
		if (blockData instanceof MultipleFacing multipleFacing) {
			if (
				multipleFacing.getFaces().contains(faceToSecure)
					&& (
					multipleFacing instanceof GlowLichen
						|| multipleFacing instanceof SculkVein
						|| Material.VINE.equals(blockMat)
				)
			) {
				return true;
			}
		} else if (blockData instanceof Directional directional) {
			BlockFace blockFace = directional.getFacing();

			if (directional instanceof Switch leverOrButton) {
				AttachedFace attachedFace = leverOrButton.getAttachedFace();
				switch (attachedFace) {
					case CEILING -> {
						if (BlockFace.UP.equals(faceToSecure)) {
							return true;
						}
					} case WALL -> {
						if (oppositeFace.equals(blockFace)) {
							return true;
						}
					}
					default -> {
					}
				}
			} else if (
				directional instanceof AmethystCluster // Handles vertical case properly
					|| directional instanceof CoralWallFan
					|| directional instanceof Ladder
					|| directional instanceof TripwireHook
					|| directional instanceof WallSign
					|| (
					blockMatKeyStr.startsWith("minecraft:")
						&& (
						blockMatKeyStr.endsWith("_wall_banner")
							|| blockMatKeyStr.endsWith("_wall_torch")
					)
				)
			) {
				// Normal (negative of sideToSecure)
				if (oppositeFace.equals(blockFace)) {
					return true;
				}
			} else if (
				directional instanceof Bell
			) {
				// Reverse of normal (sideToSecure)
				if (faceToSecure.equals(blockFace)) {
					return true;
				}
			}
		}

		// Entity on block check
		World world = testLoc.getWorld();
		Vector minBlock = testLoc.toVector();
		BoundingBox testBb = new BoundingBox(
			minBlock.getBlockX(), minBlock.getBlockY(), minBlock.getBlockZ(),
			minBlock.getBlockX() + 1, minBlock.getBlockY() + 1, minBlock.getBlockZ() + 1
		);
		for (Entity entity : world.getNearbyEntities(testBb)) {
			if (entity instanceof ItemFrame itemFrame) {
				BlockFace blockFace = itemFrame.getAttachedFace();
				if (faceToSecure.equals(blockFace)) {
					return true;
				}
			} else if (entity instanceof Painting painting) { // Includes large paintings that extend into this block
				BlockFace blockFace = painting.getAttachedFace();
				if (faceToSecure.equals(blockFace)) {
					return true;
				}
			} else if (entity instanceof Shulker shulker) {
				BlockFace blockFace = shulker.getAttachedFace();
				if (faceToSecure.equals(blockFace)) {
					return true;
				}
			}
		}

		return false;
	}

	private void secure(Location secureLoc) {
		Location aboveLoc = secureLoc.clone().add(0.0, 1.0, 0.0);
		if (Material.GLASS.equals(aboveLoc.getBlock().getType())) {
			aboveLoc.getBlock().setBlockData(Material.SAND.createBlockData(), false);
		}
		Location belowLoc = secureLoc.clone().add(0.0, -1.0, 0.0);
		// Already on a solid block, no need for glass
		if (belowLoc.getBlock().getType().isOccluding()) {
			secureLoc.getBlock().setBlockData(Material.SAND.createBlockData(), false);
			return;
		}
		secureLoc.getBlock().setBlockData(Material.GLASS.createBlockData(), false);
	}

	private boolean inAny(Vector loc, List<BoundingBox> relevantBoundingBlocks) {
		for (BoundingBox bb : relevantBoundingBlocks) {
			if (bb.contains(loc)) {
				return true;
			}
		}
		return false;
	}

	public JsonObject toJson() {
		JsonObject result = new JsonObject();

		result.addProperty("plot_number", mPlotNumber);
		result.addProperty("facing", mFacingAngle);
		result.add("origin", vectorToJson(mOrigin));
		result.add("spawn_point", vectorToJson(mSpawnPoint));
		result.add("door", bbToJson(mDoorBb));
		result.add("plot", bbArrayToJson(mPlotBbs));
		result.add("mailbox", bbArrayToJson(mMailboxBbs));
		result.add("island", bbArrayToJson(mIslandBbs));

		return result;
	}

	private static List<BoundingBox> bbArrayFromJson(JsonElement rootElement) {
		if (!(rootElement instanceof JsonArray root)) {
			throw new RuntimeException("Unexpected json data type for List<BoundingBox>");
		}

		List<BoundingBox> result = new ArrayList<>();
		for (JsonElement bbJson : root) {
			result.add(bbFromJson(bbJson));
		}
		return result;
	}

	private static JsonArray bbArrayToJson(List<BoundingBox> bbs) {
		JsonArray result = new JsonArray();

		for (BoundingBox bb : bbs) {
			result.add(bbToJson(bb));
		}

		return result;
	}

	private static BoundingBox bbFromJson(JsonElement rootElement) {
		if (!(rootElement instanceof JsonObject root)) {
			throw new RuntimeException("Unexpected json data type for BoundingBox");
		}

		return new BoundingBox(
			root.getAsJsonPrimitive("min_x").getAsDouble(),
			root.getAsJsonPrimitive("min_y").getAsDouble(),
			root.getAsJsonPrimitive("min_z").getAsDouble(),
			root.getAsJsonPrimitive("max_x").getAsDouble(),
			root.getAsJsonPrimitive("max_y").getAsDouble(),
			root.getAsJsonPrimitive("max_z").getAsDouble()
		);
	}

	private static JsonObject bbToJson(BoundingBox bb) {
		JsonObject result = new JsonObject();

		result.addProperty("min_x", bb.getMin().getBlockX());
		result.addProperty("min_y", bb.getMin().getBlockY());
		result.addProperty("min_z", bb.getMin().getBlockZ());

		result.addProperty("max_x", bb.getMax().getBlockX());
		result.addProperty("max_y", bb.getMax().getBlockY());
		result.addProperty("max_z", bb.getMax().getBlockZ());

		return result;
	}

	private static Vector vectorFromJson(JsonElement rootElement) {
		if (!(rootElement instanceof JsonArray root)) {
			throw new RuntimeException("Unexpected json data type for Vector");
		}

		List<Double> coords = new ArrayList<>();
		for (JsonElement coordElement : root) {
			if (!(coordElement instanceof JsonPrimitive coordPrimitive) || !coordPrimitive.isNumber()) {
				throw new RuntimeException("Unexpected json data type for Vector coordinate element");
			}
			coords.add(coordPrimitive.getAsDouble());
		}

		if (coords.size() != 3) {
			throw new RuntimeException("Unexpected length for Vector coordinate list");
		}

		return new Vector(coords.get(0), coords.get(1), coords.get(2));
	}

	private static JsonArray vectorToJson(Vector v) {
		JsonArray result = new JsonArray();

		result.add(v.getBlockX());
		result.add(v.getBlockY());
		result.add(v.getBlockZ());

		return result;
	}
}

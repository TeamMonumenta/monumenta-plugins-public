package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.delves.DelvesManager;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.scriptedquests.zones.Zone;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.WorldInfo;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SpawnerCountCommand {
	static final int DEFAULT_CHUNK_SCAN_RADIUS = 32;

	public static void register() {
		new CommandAPICommand("countspawners")
			.withPermission("monumenta.command.countspawners")
			.withSubcommand(
				new CommandAPICommand("count")
					.withArguments()
					.executesPlayer((player, args) -> {
						count(player, player.getWorld(), DEFAULT_CHUNK_SCAN_RADIUS, false);
					})
			)
			.withSubcommand(
				new CommandAPICommand("count")
					.withArguments(new StringArgument("world")
						               .replaceSuggestions(ArgumentSuggestions.stringCollection(info -> Bukkit.getWorlds().stream().map(WorldInfo::getName).toList())))
					.executes((sender, args) -> {
						World world = Bukkit.getWorld((String) args[0]);
						if (world == null) {
							throw CommandAPI.failWithString("Unknown world " + args[0]);
						}
						count(sender, world, DEFAULT_CHUNK_SCAN_RADIUS, false);
					})
			)
			.withSubcommand(
				new CommandAPICommand("count")
					.withArguments(new StringArgument("world")
						               .replaceSuggestions(ArgumentSuggestions.stringCollection(info -> Bukkit.getWorlds().stream().map(WorldInfo::getName).toList())))
					.withArguments(new IntegerArgument("chunk radius"))
					.executes((sender, args) -> {
						World world = Bukkit.getWorld((String) args[0]);
						if (world == null) {
							throw CommandAPI.failWithString("Unknown world " + args[0]);
						}
						count(sender, world, (Integer) args[1], false);
					})
			)
			.withSubcommand(
				new CommandAPICommand("countAndUpdate")
					.withArguments()
					.executesPlayer((player, args) -> {
						count(player, player.getWorld(), DEFAULT_CHUNK_SCAN_RADIUS, true);
					})
			)
			.withSubcommand(
				new CommandAPICommand("countAndUpdate")
					.withArguments(new StringArgument("world")
						               .replaceSuggestions(ArgumentSuggestions.stringCollection(info -> Bukkit.getWorlds().stream().map(WorldInfo::getName).toList())))
					.executes((sender, args) -> {
						World world = Bukkit.getWorld((String) args[0]);
						if (world == null) {
							throw CommandAPI.failWithString("Unknown world " + args[0]);
						}
						count(sender, world, DEFAULT_CHUNK_SCAN_RADIUS, true);
					})
			)
			.withSubcommand(
				new CommandAPICommand("countAndUpdate")
					.withArguments(new StringArgument("world")
						               .replaceSuggestions(ArgumentSuggestions.stringCollection(info -> Bukkit.getWorlds().stream().map(WorldInfo::getName).toList())))
					.withArguments(new IntegerArgument("chunk radius"))
					.executes((sender, args) -> {
						World world = Bukkit.getWorld((String) args[0]);
						if (world == null) {
							throw CommandAPI.failWithString("Unknown world " + args[0]);
						}
						count(sender, world, (Integer) args[1], true);
					})
			)
			.register();
	}

	private static final Pattern SPAWNER_COUNT_ZONE_PROPERTY_PATTERN = Pattern.compile("Spawner Counter Branch: (.+), (\\d+), (.+)");

	private record BranchSet(int mNumBranchesRequired, Map<String, Integer> mSpawnersPerBranch) {
	}

	public static void count(@Nullable CommandSender sender, World world, int radius, boolean update) {
		if (sender != null) {
			sender.sendMessage(Component.text("Counting spawners within " + radius + " chunks around the world's spawn point..."));
		}
		Location spawnLocation = world.getSpawnLocation();
		// Get starting coords (divide by 16, basically)
		int spawnChunkX = spawnLocation.getBlockX() >> 4;
		int spawnChunkZ = spawnLocation.getBlockZ() >> 4;
		int startRegionX = (spawnChunkX - radius) >> 5;
		int endRegionX = (spawnChunkX + radius) >> 5;
		int startRegionZ = (spawnChunkZ - radius) >> 5;

		long start = System.currentTimeMillis();

		AtomicInteger branchlessSpawners = new AtomicInteger(0);
		ConcurrentMap<String, BranchSet> branchSets = new ConcurrentHashMap<>();
		AtomicInteger numChunksToLoad = new AtomicInteger((2 * radius + 1) * (2 * radius + 1));

		// Load each chunk async, when they load the callback will be called
		// One region is handled at a time, after which the server is given some time to catch up to ensure that it is not stopped due not responding
		AtomicInteger regionX = new AtomicInteger(startRegionX);
		AtomicInteger regionZ = new AtomicInteger(startRegionZ);
		Runnable regionRunnable = new Runnable() {
			@Override
			public void run() {
				if (sender != null) {
					sender.sendMessage(Component.text("Starting region (" + regionX.get() + "," + regionZ.get() + ")"));
				}
				int startX = Math.max(spawnChunkX - radius, regionX.get() << 5);
				int endX = Math.min(((regionX.get() + 1) << 5) - 1, spawnChunkX + radius);
				int startZ = Math.max(spawnChunkZ - radius, regionZ.get() << 5);
				int endZ = Math.min(((regionZ.get() + 1) << 5) - 1, spawnChunkZ + radius);
				AtomicInteger numChunksInThisRegion = new AtomicInteger((endX - startX + 1) * (endZ - startZ + 1));
				for (int cx = startX; cx <= endX; cx++) {
					for (int cz = startZ; cz <= endZ; cz++) {
						world.getChunkAtAsync(cx, cz, false /* don't create new chunks */, (chunk) -> {
							if (chunk != null && chunk.isLoaded()) {
								// This gets called once per chunk
								for (BlockState tile : chunk.getTileEntities(block -> block.getType() == Material.SPAWNER, false)) {
									// Ignore spawners on bedrock, they cannot be broken (and may not even be accessible)
									if (tile.getLocation().subtract(new Vector(0, 1, 0)).getBlock().getType().equals(Material.BEDROCK)) {
										continue;
									}
									boolean hasBranchName = false;
									Optional<Zone> zone = ZoneUtils.getZone(tile.getLocation(), "spawner_counter");
									if (zone.isPresent()) {
										Set<String> properties = zone.get().getProperties();
										if (properties.contains("Spawner Counter Ignore")) {
											continue;
										}
										for (String property : properties) {
											if (property.startsWith("Spawner Counter Branch: ")) {
												Matcher m = SPAWNER_COUNT_ZONE_PROPERTY_PATTERN.matcher(property);
												if (!m.matches()) {
													continue;
												}
												hasBranchName = true;
												String setName = m.group(1);
												int numBranchesNeeded = Integer.parseInt(m.group(2));
												String branchName = m.group(3);
												branchSets.computeIfAbsent(setName, k -> new BranchSet(numBranchesNeeded, new HashMap<>()))
													.mSpawnersPerBranch.merge(branchName, 1, Integer::sum);
											}
										}
									}
									if (!hasBranchName) {
										branchlessSpawners.incrementAndGet();
									}
								}
								if (!chunk.isForceLoaded() && Arrays.stream(chunk.getEntities()).noneMatch(e -> e instanceof Player)) {
									chunk.unload();
								}
							}
							int numLeftInThisRegion = numChunksInThisRegion.decrementAndGet();
							int numLeft = numChunksToLoad.decrementAndGet();
							if (sender != null && numLeft > 0 && numLeft % 500 == 0) {
								sender.sendMessage(Component.text(numLeft + " chunks left..."));
							}
							if (numLeft == 0) {
								int numSpawners = branchlessSpawners.get();

								for (Map.Entry<String, BranchSet> e : branchSets.entrySet()) {
									BranchSet set = e.getValue();
									int spawnersInSet = set.mSpawnersPerBranch.values().stream()
										                    .mapToInt(v -> v)
										                    .sorted()
										                    .limit(set.mNumBranchesRequired)
										                    .sum();
									String branches = set.mSpawnersPerBranch.entrySet().stream()
										                  .sorted(Map.Entry.comparingByValue())
										                  .limit(set.mNumBranchesRequired)
										                  .map(Map.Entry::getKey)
										                  .collect(Collectors.joining(", "));
									if (sender != null) {
										sender.sendMessage(Component.text("Found " + spawnersInSet + " minimal spawners in set " + e.getKey() + " (using branch(es): " + branches + ")."));
									}
									numSpawners += spawnersInSet;
								}
								if (sender != null) {
									sender.sendMessage(Component.text("Found " + numSpawners + " spawners in total."));
								}
								long duration = System.currentTimeMillis() - start;
								if (sender != null && duration > 60_000) {
									sender.sendMessage(Component.text("This operation took " + StringUtils.to2DP(duration / 60_000.0) + " minutes."));
								}
								if (update) {
									world.getPersistentDataContainer().set(DelvesManager.SPAWNER_COUNT_DATA_KEY, PersistentDataType.INTEGER, numSpawners);
								}
							} else if (numLeftInThisRegion == 0) {
								if (regionX.incrementAndGet() > endRegionX) {
									regionX.set(startRegionX);
									regionZ.incrementAndGet();
								}
								Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), this, 20);
							}
						});
					}
				}
			}
		};
		regionRunnable.run();

	}

}

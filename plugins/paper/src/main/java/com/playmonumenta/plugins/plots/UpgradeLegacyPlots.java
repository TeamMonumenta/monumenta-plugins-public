package com.playmonumenta.plugins.plots;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.structures.StructuresAPI;
import com.playmonumenta.worlds.paper.MonumentaWorldManagementAPI;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.IntegerArgument;

public class UpgradeLegacyPlots {
	public static void registerCommands() {
		new CommandAPICommand("upgradelegacyplots")
			.withPermission(CommandPermission.fromString("monumenta.command.upgradelegacyplots"))
			.withArguments(new IntegerArgument("min number", 1))
			.withArguments(new IntegerArgument("max number", 1))
			.executes((sender, args) -> {
				try {
					upgrade(sender, (Integer)args[0], (Integer)args[1]);
				} catch (Exception ex) {
					CommandAPI.fail("Caught exception: " + ex.getMessage());
				}
			})
			.register();
	}

	private static boolean BUSY = false;
	private static int NUM_INFLIGHT = 0;

	private static void upgrade(CommandSender sender, int minWorld, int maxWorld) throws Exception {
		JsonObject data = getData();

		if (BUSY) {
			throw new Exception("Already running");
		}

		BUSY = true;

		Deque<Runnable> tasks = new LinkedList<>();

		for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
			JsonObject val = entry.getValue().getAsJsonObject();

			JsonArray minEle = val.get("min").getAsJsonArray();
			JsonArray maxEle = val.get("max").getAsJsonArray();

			Location min = new Location(Bukkit.getWorlds().get(0), minEle.get(0).getAsInt(), minEle.get(1).getAsInt(), minEle.get(2).getAsInt());
			Location max = new Location(Bukkit.getWorlds().get(0), maxEle.get(0).getAsInt(), maxEle.get(1).getAsInt(), maxEle.get(2).getAsInt());

			// midpoint = -1280 100 -1280
			int xmin = Math.min(min.getBlockX(), max.getBlockX());
			int zmin = Math.min(min.getBlockZ(), max.getBlockZ());
			int xmax = Math.max(min.getBlockX(), max.getBlockX());
			int zmax = Math.max(min.getBlockZ(), max.getBlockZ());

			int worldId = val.get("world_id").getAsInt();

			if (worldId < minWorld) {
				continue;
			}

			tasks.add(() -> {
				NUM_INFLIGHT += 1;
				sender.sendMessage("Plot " + worldId + " started");
				StructuresAPI.copyArea(min, max).whenComplete((clipboard, ex) -> {
					if (ex != null) {
						ex.printStackTrace();
						sender.sendMessage(ChatColor.RED + "Plot " + worldId + " failed");
						NUM_INFLIGHT--;
					} else {
						try {
							World world = MonumentaWorldManagementAPI.ensureWorldLoaded("plot" + worldId, false, true);

							Location loc = new Location(world, -1280 - ((xmax - xmin)/2), 83, -1280 - ((zmax - zmin)/2));
							StructuresAPI.pasteStructure(clipboard, loc, true).whenComplete((unused, exc) -> {
								if (exc != null) {
									exc.printStackTrace();
									sender.sendMessage(ChatColor.RED + "Plot " + worldId + " failed");
								} else {
									Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
										try {
											int y = 83;
											for (int x = -1292; x <= -1268; x++) {
												for (int z = -1292; z <= -1268; z++) {
													Block block = world.getBlockAt(x, y, z);
													if (block.getType().equals(Material.BEDROCK)) {
														block.setType(Material.SAND);
													}
												}
											}

											MonumentaWorldManagementAPI.unloadWorld(world.getName());
											sender.sendMessage("Plot " + worldId + " done and unloaded");
										} catch (Exception e) {
											e.printStackTrace();
											sender.sendMessage(ChatColor.RED + "Plot " + worldId + " failed to unload");
										}
									}, 400);
								}
								NUM_INFLIGHT--;
							});
						} catch (Exception e) {
							e.printStackTrace();
							sender.sendMessage(ChatColor.RED + "Plot " + worldId + " failed");
							NUM_INFLIGHT--;
						}
					}
				});
			});

			if (worldId >= maxWorld) {
				break;
			}
		}


		new BukkitRunnable() {
			@Override
			public void run() {
				if (tasks.isEmpty()) {
					BUSY = false;
					this.cancel();
					return;
				}

				// Past one still loading
				if (NUM_INFLIGHT >= 1) {
					return;
				}

				tasks.remove().run();
			}
		}.runTaskTimer(Plugin.getInstance(), 1, 1);
	}

	private static JsonObject getData() throws Exception {
		String playerContent = FileUtils.readFile(Plugin.getInstance().getDataFolder().getPath() + "/all_plot_records.json");
		Gson gson = new Gson();
		return gson.fromJson(playerContent, JsonObject.class);
	}
}

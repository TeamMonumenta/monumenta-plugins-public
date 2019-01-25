package com.playmonumenta.plugins.safezone;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.point.Point;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;

public class SafeZoneManager {
	public enum LocationType {
		None(-1),

		// Adventure + Resistance + Speed
		Capital(0),

		// Adventure + Resistance
		SafeZone(1),

		// Adventure
		AdventureZone(2),

		// Restricted - Adventure and some interactions are restricted
		RestrictedZone(3);

		public int mValue;
		private LocationType(int value) {
			this.mValue = value;
		}
	}

	private ArrayList<SafeZone> mLocationBounds = new ArrayList<SafeZone>();
	private Plugin mPlugin;


	public SafeZoneManager(Plugin plugin) {
		mPlugin = plugin;
		reload(null);
	}

	/* If sender is non-null, it will be sent debugging information */
	public void reload(CommandSender sender) {
		String zonesLocation = mPlugin.getDataFolder() + File.separator +  "zones";

		mLocationBounds.clear();

		ArrayList<File> listOfFiles;
		int numFiles = 0;

		// Attempt to load all JSON files in subdirectories
		try {
			File directory = new File(zonesLocation);
			if (!directory.exists()) {
				directory.mkdirs();
			}

			listOfFiles = FileUtils.getFilesInDirectory(zonesLocation, ".json");
		} catch (IOException e) {
			mPlugin.getLogger().severe("Caught exception trying to reload safezones: " + e);
			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Caught exception trying to reload safezones: " + e);
			}
			return;
		}

		Collections.sort(listOfFiles);
		for (File file : listOfFiles) {
			try {
				String content = FileUtils.readFile(file.getPath());
				if (content != null && !content.isEmpty()) {
					Gson gson = new Gson();
					JsonObject object = gson.fromJson(content, JsonObject.class);
					if (object == null) {
						throw new Exception("Unable to parse as JSON object");
					}

					// Load this file into a SafeZone object
					mLocationBounds.add(SafeZone.fromJsonObject(object));

					numFiles++;
				}
			} catch (Exception e) {
				mPlugin.getLogger().severe("Error in safezone file '" + file.getName() + "': " + e);
				e.printStackTrace();

				if (sender != null) {
					sender.sendMessage(ChatColor.RED + "Error in safezone file '" + file.getName() + "': " + e);
					MessagingUtils.sendStackTrace(sender, e);
				}
			}
		}

		if (sender != null) {
			sender.sendMessage(ChatColor.GOLD + "Loaded " + Integer.toString(numFiles) + " safezones");
		}
	}

	public String toString() {
		if (mLocationBounds.isEmpty()) {
			return "[]";
		} else {
			return mLocationBounds.toString();
		}
	}

	public LocationType getLocationType(Entity entity) {
		return getLocationType(entity.getLocation());
	}

	public LocationType getLocationType(Location location) {
		return getLocationType(new Point(location));
	}

	public LocationType getLocationType(Point point) {
		if (mPlugin.mServerProperties.getIsTownWorld()) {
			return LocationType.Capital;
		}

		for (SafeZone area : mLocationBounds) {
			if (area.within(point)) {
				return area.getType();
			}
		}

		return LocationType.None;
	}

	public boolean isInPlot(Location location) {
		if (getLocationType(location) == LocationType.Capital) {
			Material mat = location.getWorld().getBlockAt(location.getBlockX(), 10, location.getBlockZ()).getType();
			return (mat == Material.SPONGE || mat == Material.OBSIDIAN);
		}
		return false;
	}
}


package com.playmonumenta.plugins.safezone;

import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.point.Point;
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
	}

	/* If sender is non-null, it will be sent debugging information */
	public void reload(JsonElement element, CommandSender sender) {
		int numSafezones = 0;

		Iterator<JsonElement> targetIter = element.getAsJsonArray().iterator();
		while (targetIter.hasNext()) {
			JsonObject iter = targetIter.next().getAsJsonObject();

			try {
				// Load this file into a SafeZone object
				mLocationBounds.add(SafeZone.fromJsonObject(iter));
				numSafezones++;
			} catch (Exception e) {
				mPlugin.getLogger().severe("Invalid locationBounds element at: '" + iter.toString() + "'");
				e.printStackTrace();

				if (sender != null) {
					sender.sendMessage(ChatColor.RED + "Invalid locationBounds element at: '" + iter.toString() + "'");
					MessagingUtils.sendStackTrace(sender, e);
				}
			}
		}

		if (sender != null) {
			sender.sendMessage(ChatColor.GOLD + "Loaded " + Integer.toString(numSafezones) + " safezones");
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


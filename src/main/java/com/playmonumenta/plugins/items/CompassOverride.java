package com.playmonumenta.plugins.items;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.locations.poi.PointOfInterest;
import com.playmonumenta.plugins.point.Point;
import com.playmonumenta.plugins.utils.StringUtils;

public class CompassOverride extends OverrideItem {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		if (plugin.mServerProperties.getQuestCompassEnabled()) {
			//	Show current POI respawn timer.
			if (player.isSneaking()) {
				List<PointOfInterest> pois = plugin.mPOIManager.getAllNearbyPOI(new Point(player.getLocation()));
				if (pois != null && pois.size() > 0) {
					for (PointOfInterest poi : pois) {
						if (poi.getCustomMessage() != null) {
							player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + poi.getCustomMessage());
						} else {
							int ticks = poi.getTimer();
							String message;

							//	Seems there's plenty of time before we respawn.
							if (ticks >= 20) {
								message = ChatColor.GREEN + "" + ChatColor.BOLD +  poi.getName() + " is respawning in " + StringUtils.ticksToTime(ticks);
							}
							//	Because we need to handle the case where the player clicks within sub one second and we still
							//	Want to be able to tell them the POI is about to respawn while still having the [within] tag.
							else if (ticks > 0) {
								message = ChatColor.GREEN + "" + ChatColor.BOLD +  poi.getName() + " is nearly ready to respawn!";
							}
							//	We're nearby, but not within the POI
							else {
								message = ChatColor.GREEN + "" + ChatColor.BOLD +  poi.getName() + " is ready to respawn!";
							}

							if (poi.withinPOI(new Point(player.getLocation()))) {
								message += " [Within]";
							}

							player.sendMessage(message);
						}
					}
				} else {
					player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You are not within range of a Point of Interest.");
				}
			}
		} else {
			player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No interesting places on this server shard.");
		}

		return true;
	}
}

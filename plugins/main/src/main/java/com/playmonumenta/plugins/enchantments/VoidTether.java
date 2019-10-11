package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class VoidTether implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Void Tether";
	private static final Map<Player, Location> PLAYER_LOCS = new HashMap<Player, Location>();

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean useEnchantLevels() {
		return false;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.HAND);
	}

	@Override
	public void tick(Plugin plugin, World world, Player player, int level) {
		Location ploc = player.getLocation();
		if (ploc.getY() > 0) {
			/* Not in the void - clear player's location */
			PLAYER_LOCS.remove(player);
		} else {
			/* In the void - remember the highest-most Y value location (closest point to where they fell in) */
			Location lastPlayerLoc = PLAYER_LOCS.get(player);
			if (lastPlayerLoc == null || lastPlayerLoc.getY() < ploc.getY()) {
				lastPlayerLoc = ploc;
				PLAYER_LOCS.put(player, lastPlayerLoc);
			}
		}
	}

	@Override
	public void onHurt(Plugin plugin, Player player, int level, EntityDamageEvent event) {
		Location ploc = player.getLocation();

		if (ploc.getY() > 0) {
			/* Not in the void */
			return;
		}

		if (event.getFinalDamage() + 5 < player.getHealth()) {
			/* Non-fatal damage */
			return;
		}

		/* Check to make sure the player still has a totem */
		ItemStack totemItem = player.getInventory().getItemInMainHand();
		if (totemItem == null || !totemItem.getType().equals(Material.TOTEM_OF_UNDYING)) {
			totemItem = player.getInventory().getItemInOffHand();
			if (totemItem == null || !totemItem.getType().equals(Material.TOTEM_OF_UNDYING)) {
				/* Nope, no totem */
				return;
			}
		}

		/* Activate! */
		Location lastPlayerLoc = PLAYER_LOCS.get(player);
		if (lastPlayerLoc == null || lastPlayerLoc.getY() < ploc.getY()) {
			lastPlayerLoc = ploc;
		}

		/* Teleport the player to the nearest location to where they crossed into the void */
		Location saveLoc = getNearestVoidCrossing(lastPlayerLoc, 5);
		saveLoc.getBlock().setType(Material.OBSIDIAN);
		saveLoc.clone().add(0, 1, 0).getBlock().setType(Material.AIR);
		saveLoc.clone().add(0, 2, 0).getBlock().setType(Material.AIR);
		player.teleport(saveLoc.clone().add(0, 1, 0));
		player.setVelocity(new Vector(0, 0, 0));
		player.setFallDistance(0);
		player.setNoDamageTicks(20);

		/* Kill the player to trigger the totem */
		event.setDamage(1000);
	}

	private static Location getNearestVoidCrossing(Location center, int radius) {
		int cx = center.getBlockX();
		int cz = center.getBlockZ();
		World world = center.getWorld();
		Location nearest = center.clone(); /* Return center if no matches */
		double nearestDistance = Double.MAX_VALUE;

		for (double x = cx - radius; x <= cx + radius; x++) {
			for (double z = cz - radius; z <= cz + radius; z++) {
				Location loc = new Location(world, x, 0, z);
				double distance = Math.sqrt(((cx - x) * (cx - x)) + ((cz - z) * (cz - z)));
				if (distance < nearestDistance) {
					if (!loc.getBlock().getType().isSolid()) {
						// Found a valid non-solid location
						nearest = loc;
						nearestDistance = distance;
						break;
					}
				}
			}
		}
		return nearest;
	}
}

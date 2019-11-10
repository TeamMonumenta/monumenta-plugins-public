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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.ItemUtils;

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
		return EnumSet.of(ItemSlot.OFFHAND, ItemSlot.MAINHAND);
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

		if (event.getFinalDamage() < player.getHealth()) {
			/* Non-fatal damage */
			return;
		}

		/* Check to make sure the player still has a non-shattered totem */
		ItemStack totemItem = player.getInventory().getItemInMainHand();
		if (totemItem == null || !totemItem.getType().equals(Material.TOTEM_OF_UNDYING) || ItemUtils.isItemShattered(totemItem)) {
			totemItem = player.getInventory().getItemInOffHand();
			if (totemItem == null || !totemItem.getType().equals(Material.TOTEM_OF_UNDYING) || ItemUtils.isItemShattered(totemItem)) {
				/* Nope, no totem or it is shattered */
				return;
			}
		}

		if (ploc.getY() > 0) {
			plugin.getLogger().info("Player '" + player.getName() + "' was not in the void when taking fatal damage and carrying a void totem");
			/* Player took fatal damage but is not in the void */

			if (event.getFinalDamage() >= player.getHealth() && !player.isOnGround()) {
				/* Player really is going to die and is not on the ground - put them on a block */
				Location saveLoc = new Location(ploc.getWorld(), ploc.getBlockX(), Math.max(0, ploc.getBlockY() - 1), ploc.getBlockZ());
				if (saveLoc.getBlock().isPassable()) {
					/* Since they're not dying in the void, only set the block if it's already not solid */
					saveLoc.getBlock().setType(Material.OBSIDIAN);
				}
				player.setVelocity(new Vector(0, 0, 0));
				player.setFallDistance(0);
				player.setNoDamageTicks(20);
				player.teleport(saveLoc.clone().add(0.5, 1, 0.5));
			}

			return;
		}

		plugin.getLogger().info("Player '" + player.getName() + "' was in the void when taking fatal damage and carrying a void totem");

		/* In the void and taking near-fatal damage - activate! */
		Location lastPlayerLoc = PLAYER_LOCS.get(player);
		if (lastPlayerLoc == null || lastPlayerLoc.getY() < ploc.getY()) {
			lastPlayerLoc = ploc;
		}
		Location saveLoc = getNearestVoidCrossing(lastPlayerLoc, 5);

		/* Don't let the player die in this event */
		event.setCancelled(true);

		plugin.getLogger().info("Activating void tether for player '" + player.getName() + "'");

		/* Teleport the player to the nearest location to where they crossed into the void and let above logic catch them */
		player.setVelocity(new Vector(0, 0, 0));
		player.setFallDistance(0);
		player.teleport(saveLoc.clone().add(0.5, 1, 0.5));

		new BukkitRunnable() {
			@Override
			public void run() {
				/* Kill the player shortly after, triggering the totem */
				player.damage(100);
			}
		}.runTaskLater(plugin, 0);
	}

	private static Location getNearestVoidCrossing(Location center, int radius) {
		int cx = center.getBlockX();
		int cz = center.getBlockZ();
		World world = center.getWorld();
		Location nearest = new Location(world, cx, 0, cz); /* Return center if no matches */
		double nearestDistance = Double.MAX_VALUE;

		for (double x = cx - radius; x <= cx + radius; x++) {
			for (double z = cz - radius; z <= cz + radius; z++) {
				Location loc = new Location(world, x, 0, z);
				double distance = Math.sqrt(((cx - x) * (cx - x)) + ((cz - z) * (cz - z)));
				if (distance < nearestDistance) {
					if (loc.getBlock().isPassable()) {
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

package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VoidTether implements Enchantment {
	private static final Map<UUID, Location> PLAYER_LOCS = new HashMap<>();

	@Override
	public String getName() {
		return "Void Tether";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.VOID_TETHER;
	}

	@Override
	public void tick(Plugin plugin, Player player, double level, boolean twoHz, boolean oneHz) {
		Location ploc = player.getLocation();
		if (ploc.getY() > 0) {
			/* Not in the void - clear player's location */
			PLAYER_LOCS.remove(player.getUniqueId());
		} else {
			/* In the void - remember the highest-most Y value location (closest point to where they fell in) */
			Location lastPlayerLoc = PLAYER_LOCS.get(player.getUniqueId());
			if (lastPlayerLoc == null || lastPlayerLoc.getY() < ploc.getY()) {
				lastPlayerLoc = ploc;
				PLAYER_LOCS.put(player.getUniqueId(), lastPlayerLoc);
			}
		}
	}

	protected boolean runCheck(Plugin plugin, Player player) {
		/* Check to make sure the player still has a non-shattered totem or resurrection item */
		if (plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.RESURRECTION) == 0) {
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item == null || !item.getType().equals(Material.TOTEM_OF_UNDYING) || ItemStatUtils.isShattered(item)) {
				item = player.getInventory().getItemInOffHand();
				if (item == null || !item.getType().equals(Material.TOTEM_OF_UNDYING) || ItemStatUtils.isShattered(item)) {
					/* Nope, no totem or resurrection item or it is shattered */
					return false;
				}
			}
		}
		return true;
	}

	/* Note - if the player dies in the void, this function will run twice. The first time
	 * it will prevent the damage and teleport the player, then kill them again so it runs again,
	 * processing them up above the void
	 */
	@Override
	public void onHurtFatal(Plugin plugin, Player player, double level, DamageEvent event) {
		if (event.isCancelled() || !runCheck(plugin, player)) {
			return;
		}

		Location ploc = player.getLocation();
		if (ploc.getY() > 0) {
			/* Player took fatal damage but is not in the void */

			if (!player.isOnGround()) {
				/* Player really is going to die and is not on the ground - put them on a block */
				Location saveLoc = new Location(ploc.getWorld(), ploc.getBlockX(), Math.max(0, ploc.getBlockY() - 1), ploc.getBlockZ());
				if (saveLoc.getBlock().isPassable() && player.getGameMode().equals(GameMode.SURVIVAL)) {
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

		/* In the void and taking near-fatal damage - activate! */
		Location lastPlayerLoc = PLAYER_LOCS.get(player.getUniqueId());
		if (lastPlayerLoc == null || lastPlayerLoc.getY() < ploc.getY()) {
			lastPlayerLoc = ploc;
		}
		Location saveLoc = getNearestVoidCrossing(lastPlayerLoc, 5);

		/* Don't let the player die in this event */
		event.setCancelled(true);

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

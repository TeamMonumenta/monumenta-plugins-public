package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import javax.annotation.Nullable;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
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
	public double getPriorityAmount() {
		return 9990; // before Resurrection, after Ashes of Eternity
	}

	@Override
	public void tick(Plugin plugin, Player player, double level, boolean twoHz, boolean oneHz) {
		tick(player);
	}

	public static void tick(Player player) {
		Location ploc = player.getLocation();
		if (ploc.getY() > 0) {
			/* Not in the void - clear player's location */
			PLAYER_LOCS.remove(player.getUniqueId());
		} else {
			/* In the void - remember the highest-most Y value location (closest point to where they fell in) */
			Location lastPlayerLoc = PLAYER_LOCS.get(player.getUniqueId());
			if (lastPlayerLoc == null || lastPlayerLoc.getY() < ploc.getY()) {
				PLAYER_LOCS.put(player.getUniqueId(), ploc);
			}
		}
	}

	@Override
	public void onHurtFatal(Plugin plugin, Player player, double level, DamageEvent event) {

		execute(plugin, player, event, getEnchantmentType());

	}

	public static boolean execute(Plugin plugin, Player player, DamageEvent event, @Nullable EnchantmentType resurrectionEnchantment) {

		if (player.getLocation().getY() >= 0) {
			return false;
		}

		// perform normal resurrection
		if (!Resurrection.execute(plugin, player, event, resurrectionEnchantment)) {
			return false;
		}

		// Teleport the player to the nearest location to where they crossed into the void, and create a block of obsidian below them
		Location lastPlayerLoc = PLAYER_LOCS.get(player.getUniqueId());
		Location ploc = player.getLocation();
		if (lastPlayerLoc == null || lastPlayerLoc.getY() < ploc.getY()) {
			lastPlayerLoc = ploc;
		}
		Location saveLoc = getNearestVoidCrossing(lastPlayerLoc, 5);

		if (!saveLoc.getBlock().isSolid() && player.getGameMode().equals(GameMode.SURVIVAL)) {
			saveLoc.getBlock().setType(Material.OBSIDIAN);
		}

		player.setVelocity(new Vector(0, 0, 0));
		player.setFallDistance(0);
		player.setNoDamageTicks(20);
		player.teleport(saveLoc.clone().add(0.5, 1, 0.5));

		player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1);
		player.getWorld().spawnParticle(Particle.TOTEM, player.getLocation().add(0, 1, 0), 100, 0, 0, 0, 1);

		return true;

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
					if (loc.getBlock().isPassable() && loc.clone().add(0, 1, 0).getBlock().isPassable()) {
						// Found a valid non-solid location
						nearest = loc;
						nearestDistance = distance;
					}
				}
			}
		}
		return nearest;
	}


}

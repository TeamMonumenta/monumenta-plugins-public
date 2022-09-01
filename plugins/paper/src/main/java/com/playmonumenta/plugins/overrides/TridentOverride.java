package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRiptideEvent;

public class TridentOverride extends BaseOverride {
	@Override
	public boolean playerRiptide(Plugin plugin, Player player, PlayerRiptideEvent event) {
		if (player == null) {
			return true;
		}
		if (player.getInventory().getItemInMainHand().getType() != Material.TRIDENT &&
				player.getInventory().getItemInOffHand().getType() != Material.TRIDENT) {
			return false;
		}

		if (event.getItem().getEnchantmentLevel(Enchantment.RIPTIDE) > 0) {
			//Checks in a 3x3 around the player's eye location for water
			Location eyeLoc = player.getEyeLocation();
			int radius = 1;
			for (double x = eyeLoc.getX() - radius; x <= eyeLoc.getX() + radius; x++) {
				for (double y = eyeLoc.getY() - radius; y <= eyeLoc.getY() + radius; y++) {
					for (double z = eyeLoc.getZ() - radius; z <= eyeLoc.getZ() + radius; z++) {
						if (LocationUtils.isLocationInWater(new Location(player.getWorld(), x, y, z))) {
							return true;
						}
					}
				}
			}

			//Checks in a 3x3 around the player's location for water
			Location loc = player.getLocation();
			for (double x = loc.getX() - radius; x <= loc.getX() + radius; x++) {
				for (double y = loc.getY() - radius; y <= loc.getY() + radius; y++) {
					for (double z = loc.getZ() - radius; z <= loc.getZ() + radius; z++) {
						if (LocationUtils.isLocationInWater(new Location(player.getWorld(), x, y, z))) {
							return true;
						}
					}
				}
			}

			return false;
		}
		return false;
	}
}

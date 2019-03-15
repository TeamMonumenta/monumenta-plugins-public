package com.playmonumenta.plugins.overrides;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.safezone.SafeZoneManager.LocationType;

public class StairsOverride extends BaseOverride {
	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		if ((block.getBlockData() instanceof Stairs) && (item == null || item.getType().equals(Material.AIR))) {
			LocationType zone = plugin.mSafeZoneManager.getLocationType(player);
			if (zone == LocationType.Capital || zone == LocationType.SafeZone)  {
				Stairs data = (Stairs)block.getBlockData();
				Location loc = block.getLocation().add(0.5, -1.2, 0.5);
				Vector dir = data.getFacing().getOppositeFace().getDirection().setY(0).normalize();
				loc.add(dir.multiply(0.1));
				loc.setDirection(data.getFacing().getOppositeFace().getDirection());

				sitOnLocation(plugin, player, loc, block);
				return false;
			}
		}
		return true;
	}

	/* TODO: This needs to track all armor stands and remove them when the plugin is stopped! */
	protected static void sitOnLocation(Plugin plugin, Player player, Location loc, Block block) {
		Location pLoc = player.getLocation();

		ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
		player.getWorld().playSound(loc, Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 0.6f);
		stand.setVisible(false);
		stand.setInvulnerable(true);
		stand.setGravity(false);
		stand.addPassenger(player);
		stand.addScoreboardTag("for_sitting");

		new BukkitRunnable() {
			@Override
			public void run() {
				if (!stand.isValid() || !stand.getPassengers().contains(player) || block.getType() == Material.AIR || !player.isValid()) {
					this.cancel();
					stand.remove();
					if (!player.isSleeping()) {
						player.teleport(pLoc);
					}
					player.getWorld().playSound(loc, Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
				}
			}
		}.runTaskTimer(plugin, 0, 1);
	}
}

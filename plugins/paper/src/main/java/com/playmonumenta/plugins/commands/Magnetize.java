package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Collection;
import java.util.List;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Magnetize extends GenericCommand {

	public static final double MOB_RADIUS = 8.0;
	public static final double PICKUP_RADIUS = 16.0;

	public static void register() {

		registerPlayerCommand("magnetize", "monumenta.command.magnetize",
		                      (sender, player) -> run(player));
	}

	private static void run(Player player) {

		Collection<Item> nearbyItems = player.getLocation().getNearbyEntitiesByType(Item.class, PICKUP_RADIUS);

		if (nearbyItems.isEmpty()) {
			player.sendMessage("No nearby death pile items found!");
			return;
		}
		//Get nearby mobs to the player to check for near items
		List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(player.getLocation(), MOB_RADIUS + PICKUP_RADIUS);
		//Total count of items restored
		int restored = 0;
		//Loop through nearby items and send them to player if they match their death pile
		for (Item i : nearbyItems) {

			ItemStack stack = i.getItemStack();

			if (stack.getType().isAir() || !player.getUniqueId().equals(i.getOwner())) {
				continue;
			}

			//Check to see if any of the mobs around the player are too close to the item
			if (nearbyMobs.size() > 0) {
				boolean closeMob = false;
				for (LivingEntity mob : nearbyMobs) {
					if (mob.getLocation().distanceSquared(i.getLocation()) < MOB_RADIUS * MOB_RADIUS) {
						closeMob = true;
						break;
					}
				}

				if (closeMob) {
					continue;
				}
			}
			//Restore item to player
			i.teleport(player.getLocation());
			restored++;
		}
		//Get item player is using to call this method
		ItemStack mainhand = player.getInventory().getItemInMainHand();

		//If we restored items, consume item in mainhand
		if (restored > 0 && mainhand.getAmount() > 0) {
			mainhand.setAmount(mainhand.getAmount() - 1);
		} else {
			player.sendMessage("No nearby death pile items found! They might be near mobs!");
		}
	}
}

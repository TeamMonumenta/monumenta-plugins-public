package pe.project.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import pe.project.Main;
import pe.project.utils.InventoryUtils;

public class MobListener implements Listener {
	Main mPlugin = null;

	public MobListener(Main plugin) {
		mPlugin = plugin;
	}

	/**
	 * Items drop if they have lore that does not contain $$$
	 */
	private float _getItemDropChance(ItemStack item) {
		if ((item.hasItemMeta() && item.getItemMeta().hasLore()) && !InventoryUtils.testForItemWithLore(item, "$$$")) {
			return 1.0f;
		} else {
			return -200.0f;
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	void CreatureSpawnEvent(CreatureSpawnEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof Monster) {
			Monster mob = (Monster)entity;

			//	Mark mobs not able to pick-up items.
			mob.setCanPickupItems(false);

			// Overwrite drop chances for mob armor and held items
			EntityEquipment equipment = mob.getEquipment();
			equipment.setHelmetDropChance(_getItemDropChance(equipment.getHelmet()));
			equipment.setChestplateDropChance(_getItemDropChance(equipment.getChestplate()));
			equipment.setLeggingsDropChance(_getItemDropChance(equipment.getLeggings()));
			equipment.setBootsDropChance(_getItemDropChance(equipment.getBoots()));
			equipment.setItemInMainHandDropChance(_getItemDropChance(equipment.getItemInMainHand()));
			equipment.setItemInOffHandDropChance(_getItemDropChance(equipment.getItemInOffHand()));
		}
	}
}

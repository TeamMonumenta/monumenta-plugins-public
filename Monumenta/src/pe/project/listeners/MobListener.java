package pe.project.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import pe.project.Main;

public class MobListener implements Listener {
	Main mPlugin = null;

	public MobListener(Main plugin) {
		mPlugin = plugin;
	}

	/**
	 * Returns true if the ItemStack has lore not containing '$$$' and should be dropped
	 */
	private boolean _shouldItemDrop(ItemStack item) {
		if (item != null && item.hasItemMeta() == true) {
			ItemMeta meta = item.getItemMeta();
			if (meta.hasLore() == true) {
				for (String s: meta.getLore()) {
					if (s.contains("$$$")) {
						continue;
					} else {
						return true;
					}
				}
			}
		}
		return false;
	}

	@EventHandler(priority = EventPriority.HIGH)
	void CreatureSpawnEvent(CreatureSpawnEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof Monster) {
			Monster mob = (Monster)entity;

			//	Mark mobs not able to pick-up items.
			mob.setCanPickupItems(false);

			//	Set all drop chances on gear and in main/off hand to be 0.
			EntityEquipment equipment = mob.getEquipment();

			if (_shouldItemDrop(equipment.getHelmet())) {
				equipment.setHelmetDropChance(1.0f);
			} else {
				equipment.setHelmetDropChance(0.0f);
			}

			if (_shouldItemDrop(equipment.getChestplate())) {
				equipment.setChestplateDropChance(1.0f);
			} else {
				equipment.setChestplateDropChance(0.0f);
			}

			if (_shouldItemDrop(equipment.getLeggings())) {
				equipment.setLeggingsDropChance(1.0f);
			} else {
				equipment.setLeggingsDropChance(0.0f);
			}

			if (_shouldItemDrop(equipment.getBoots())) {
				equipment.setBootsDropChance(1.0f);
			} else {
				equipment.setBootsDropChance(0.0f);
			}

			if (_shouldItemDrop(equipment.getItemInMainHand())) {
				equipment.setItemInMainHandDropChance(1.0f);
			} else {
				equipment.setItemInMainHandDropChance(0.0f);
			}

			if (_shouldItemDrop(equipment.getItemInOffHand())) {
				equipment.setItemInOffHandDropChance(1.0f);
			} else {
				equipment.setItemInOffHandDropChance(0.0f);
			}
		}
	}
}

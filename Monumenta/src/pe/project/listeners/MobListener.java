package pe.project.listeners;

import java.util.Set;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;

import pe.project.Main;

public class MobListener implements Listener {
	Main mPlugin = null;
	
	public MobListener(Main plugin) {
		mPlugin = plugin;
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
			
			equipment.setHelmetDropChance(0.0f);
			equipment.setChestplateDropChance(0.0f);
			equipment.setLeggingsDropChance(0.0f);
			equipment.setBootsDropChance(0.0f);
				
			Set<String> tags = mob.getScoreboardTags();
			if (!tags.contains("Elite")) {
				equipment.setItemInMainHandDropChance(0.0f);
				equipment.setItemInOffHandDropChance(0.0f);
			}
		}
	}
}

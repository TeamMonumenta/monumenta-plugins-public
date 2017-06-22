package pe.project.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;

import pe.project.Main;
import pe.project.utils.InventoryUtils;

public class ItemListener implements Listener {
	Main mPlugin = null;
	
	public ItemListener(Main plugin) {
		mPlugin = plugin;
	}
	
	//	Prevent those pesky $$$ Items
	@EventHandler(priority = EventPriority.LOWEST)
	public void ItemSpawnEvent(ItemSpawnEvent event) {
		ItemStack item = event.getEntity().getItemStack();
		if (InventoryUtils.testForItemWithLore(item, "$$")) {
			event.setCancelled(true);
		}
	}
}

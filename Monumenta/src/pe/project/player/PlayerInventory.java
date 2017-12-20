package pe.project.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;

import pe.project.Plugin;
import pe.project.item.properties.ItemProperty;
import pe.project.item.properties.ItemPropertyManager;
import pe.project.utils.InventoryUtils;

public class PlayerInventory {
	HashMap<EquipmentSlot, List<ItemProperty>> mInventoryProperties = new HashMap<EquipmentSlot, List<ItemProperty>>();
	boolean mHasTickingProperty = false;

	public PlayerInventory(Plugin plugin, Player player) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			mInventoryProperties.put(slot, new ArrayList<ItemProperty>());
		}

		updateEquipmentProperties(plugin, player);
	}

	public void tick(Plugin plugin, World world, Player player) {
		//	If there is no ticking property on our gear early out.
		if (!mHasTickingProperty) {
			return;
		}

		Iterator<Entry<EquipmentSlot, List<ItemProperty>>> iter = mInventoryProperties.entrySet().iterator();
		while (iter.hasNext()) {
			List<ItemProperty> list = iter.next().getValue();

			for (ItemProperty property : list) {
				property.tick(plugin, world, player);
			}
		}
	}

	public void updateEquipmentProperties(Plugin plugin, Player player) {
		//	Loop through existing equipment properties, remove all necessary ones.
		cleanupProperties(plugin, player);

		//	Once that's done, loop through the current players inventory and re-register the properties.
		_getEquipmentProperties(plugin, player);
	}

	public void cleanupProperties(Plugin plugin, Player player) {
		mHasTickingProperty = false;

		Iterator<Entry<EquipmentSlot, List<ItemProperty>>> iter = mInventoryProperties.entrySet().iterator();
		while (iter.hasNext()) {
			List<ItemProperty> properties = iter.next().getValue();
			for (ItemProperty p : properties) {
				p.removeProperty(plugin, player);
			}

			properties.clear();
		}
	}

	private void _getEquipmentProperties(Plugin plugin, Player player) {
		final EntityEquipment equipment = player.getEquipment();

		Iterator<Entry<EquipmentSlot, List<ItemProperty>>> iter = mInventoryProperties.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<EquipmentSlot, List<ItemProperty>> entry = iter.next();
			EquipmentSlot slot = entry.getKey();

			final List<ItemProperty> properties = ItemPropertyManager.getItemProperties(InventoryUtils.getItemFromEquipment(equipment, slot), slot, player);
			for (ItemProperty p : properties) {
				p.applyProperty(plugin, player);

				if (p.hasTickingEffect()) {
					mHasTickingProperty = true;
				}
			}

			entry.setValue(properties);
		}
	}
}

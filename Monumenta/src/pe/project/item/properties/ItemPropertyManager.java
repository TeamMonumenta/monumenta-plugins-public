package pe.project.item.properties;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import pe.project.utils.InventoryUtils;

public class ItemPropertyManager {
	static List<ItemProperty> mProperties = new ArrayList<ItemProperty>();

	//  Static list of Item Properties.
	static {
		mProperties.add(new Regeneration());
		mProperties.add(new MainhandRegeneration());
		mProperties.add(new Darksight());
		mProperties.add(new Radiant());
		mProperties.add(new Gills());
		mProperties.add(new Stylish());
	}

	public static List<ItemProperty> getItemProperties(ItemStack item, EquipmentSlot slot) {
		List<ItemProperty> properties = new ArrayList<ItemProperty>();

		for (ItemProperty property : mProperties) {
			if (property.validSlot(slot)
			    && InventoryUtils.testForItemWithLore(item, property.getProperty())) {
				properties.add(property);
			}
		}

		return properties;
	}
}

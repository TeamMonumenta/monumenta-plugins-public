package com.playmonumenta.plugins.item.properties;

import java.util.ArrayList;
import java.util.Map;
import java.util.EnumMap;
import java.util.List;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.playmonumenta.plugins.Plugin;

public class ItemPropertyManager {
	/* NOTE:
	 *
	 * It is important that the specified slots do not overlap. That means you can have an item property
	 * that works in MAINHAND and ARMOR, but not one that has both MAINHAND and INVENTORY because that item
	 * will be checked twice.
	 */
	public enum ItemSlot {
		MAINHAND,
		OFFHAND,
		HAND,      // Includes both offhand and mainhand
		ARMOR,     // Does NOT include offhand!
		INVENTORY, // Includes everything, including armor, offhand, and hotbar
	}

	/*
	 * Keep a static map of which properties apply to which slots
	 * This can dramatically reduce the number of checks done when looking through player inventories.
	 * Most items only apply to armor or mainhand, and we don't need to check every single item in the
	 * player's inventory * against these properties every time the player's inventory changes
	 */
	private static Map<ItemSlot, List<ItemProperty>> mProperties
	    = new EnumMap<ItemSlot, List<ItemProperty>>(ItemSlot.class);
	private static List<ItemProperty> mSpawnedProperties = new ArrayList<ItemProperty>();

	//  Static list of Item Properties.
	static {
		List<ItemProperty> init = new ArrayList<ItemProperty>();
		init.add(new Regeneration());
		init.add(new MainhandRegeneration());
		init.add(new Darksight());
		init.add(new Radiant());
		init.add(new Gills());
		init.add(new Gilded());
		init.add(new Clucking());
		init.add(new Stylish());
		init.add(new Hope());
		init.add(new Frost());
		init.add(new Intuition());
		init.add(new LifeDrain());

		init.add(new CurseOfCorruption());

		init.add(new Chaotic());
		init.add(new IceAspect());
		init.add(new Slayer());
		init.add(new Thunder());

		/* Build the map of which slots have which properties */
		for (ItemProperty property : init) {
			for (ItemSlot slot : property.validSlots()) {
				List<ItemProperty> slotList = mProperties.get(slot);
				if (slotList == null) {
					slotList = new ArrayList<ItemProperty>();
				}
				slotList.add(property);
				mProperties.put(slot, slotList);
			}

			if (property.hasOnSpawn()) {
				mSpawnedProperties.add(property);
			}
		}
	}

	private static ItemStack[] _getItems(PlayerInventory inv, ItemSlot slot) {
		switch (slot) {
		case MAINHAND:
			return new ItemStack[] {inv.getItemInMainHand()};
		case OFFHAND:
			return new ItemStack[] {inv.getItemInOffHand()};
		case ARMOR:
			return inv.getArmorContents();
		case INVENTORY:
			return inv.getContents();
		case HAND:
			return new ItemStack[] {inv.getItemInMainHand(), inv.getItemInOffHand()};
		}

		return null;
	}

	/*
	 * Build a map containing all of the ItemProperty's that apply to a player and the
	 * cumulative level of each property
	 *
	 * propertyMap is passed in by reference and updated by this function
	 *
	 * NOTE: propertyMap should already be clear'ed and the properties removed from the player
	 */
	public static void getItemProperties(Map<ItemProperty, Integer>propertyMap, Player player) {
		final PlayerInventory inv = player.getInventory();

		/* Step over the slots for which we have item properties */
		for (Map.Entry<ItemSlot, List<ItemProperty>> entry : mProperties.entrySet()) {
			ItemSlot slot = entry.getKey();
			List<ItemProperty> slotProperties = entry.getValue();

			if (slotProperties != null && !(slotProperties.isEmpty())) {
				/* Step over the item(s) the player has in that slot */
				for (ItemStack item : _getItems(inv, slot)) {
					/* Step over the properties that apply to that slot */
					for (ItemProperty property : slotProperties) {
						/*
						 * If this particular property applies levels,
						 * add them to the running count
						 */
						int level = property.getLevelFromItem(item, player);
						if (level > 0) {
							Integer currentLevel = propertyMap.get(property);
							if (currentLevel != null) {
								currentLevel += level;
							} else {
								currentLevel = level;
							}
							propertyMap.put(property, currentLevel);
						}
					}
				}
			}
		}
	}

	/*
	 * Check if the newly spawned item entity matches any of the spawned item properties
	 */
	public static void ItemSpawnEvent(Plugin plugin, Item item) {
		if (item != null) {
			ItemStack stack = item.getItemStack();
			if (stack != null) {
				for (ItemProperty property : mSpawnedProperties) {
					int level = property.getLevelFromItem(stack);
					if (level > 0) {
						property.onSpawn(plugin, item, level);
					}
				}
			}
		}
	}
}

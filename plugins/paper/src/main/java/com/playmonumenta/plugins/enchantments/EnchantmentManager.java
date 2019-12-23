package com.playmonumenta.plugins.enchantments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.evasions.AbilityEvasion;
import com.playmonumenta.plugins.enchantments.evasions.Evasion;
import com.playmonumenta.plugins.enchantments.evasions.MeleeEvasion;
import com.playmonumenta.plugins.enchantments.evasions.SecondWind;
import com.playmonumenta.plugins.utils.ItemUtils;

public class EnchantmentManager implements Listener {
	/* NOTE:
	 *
	 * It is important that the specified slots do not overlap. That means you can have an item property
	 * that works in MAINHAND and ARMOR, but not one that has both MAINHAND and INVENTORY because that item
	 * will be checked twice.
	 */
	public enum ItemSlot {
		MAINHAND,
		OFFHAND,
		ARMOR,     // Does NOT include offhand!
		INVENTORY, // Includes everything, including armor, offhand, and hotbar
		NONE,
	}

	/*
	 * Keep a map of which properties apply to which slots
	 * This can dramatically reduce the number of checks done when looking through player inventories.
	 * Most items only apply to armor or mainhand, and we don't need to check every single item in the
	 * player's inventory * against these properties every time the player's inventory changes
	 */
	private final Map<ItemSlot, List<BaseEnchantment>> mProperties = new EnumMap<ItemSlot, List<BaseEnchantment>>(ItemSlot.class);
	private final List<BaseEnchantment> mSpawnedProperties = new ArrayList<BaseEnchantment>();
	private final Plugin mPlugin;

	public EnchantmentManager(Plugin plugin) {
		mPlugin = plugin;
	}

	public void load(Collection<String> forbiddenItemLore) {
		mProperties.clear();
		mSpawnedProperties.clear();

		final List<BaseEnchantment> init = new ArrayList<BaseEnchantment>();

		// Passive enchantments
		init.add(new Regeneration());
		init.add(new Darksight());
		init.add(new Radiant());
		init.add(new Gills());
		init.add(new Gilded());
		init.add(new Festive());
		init.add(new Clucking());
		init.add(new Oinking());
		init.add(new Stylish());
		init.add(new Colorful());
		init.add(new Hope());
		init.add(new Frost());
		init.add(new Intuition());
		init.add(new LifeDrain());
		init.add(new Evasion());
		init.add(new MeleeEvasion());
		init.add(new AbilityEvasion());
		init.add(new Resurrection());
		init.add(new DivineAura());
		init.add(new SecondWind());
		init.add(new VoidTether());
		init.add(new Ethereal());

		// Active enchantments
		init.add(new Chaotic());
		init.add(new IceAspect());
		init.add(new Slayer());
		init.add(new Duelist());
		init.add(new Thunder());
		init.add(new Inferno());
		init.add(new Impact());
		init.add(new AttributeArrowSpeed());
		init.add(new AttributeBowDamage());
		init.add(new Sniper());
		init.add(new PointBlank());
		init.add(new Decay());
		init.add(new Sapper());
		init.add(new Multitool());
		init.add(new HexEater());
		init.add(new ThrowingKnife());
		init.add(new Current());
		init.add(new InstantDrink());
		init.add(new JunglesNourishment());
		init.add(new Adrenaline());
		init.add(new Sustenance());

		// Curses
		init.add(new CurseOfCorruption());
		init.add(new TwoHanded());
		init.add(new CurseOfCrippling());
		init.add(new CurseOfEphemerality());

		// Tesseracts (not actually items a player can get enchants from)
		init.add(new PestilenceTesseract());

		// Forbidden items (dynamically set based on server config)
		if (forbiddenItemLore != null && !forbiddenItemLore.isEmpty()) {
			for (String str : forbiddenItemLore) {
				init.add(new ForbiddenItem(str));
			}
		}

		/* Build the map of which slots have which properties */
		for (BaseEnchantment property : init) {
			for (ItemSlot slot : property.validSlots()) {
				List<BaseEnchantment> slotList = mProperties.get(slot);
				if (slotList == null) {
					slotList = new ArrayList<BaseEnchantment>();
				}
				slotList.add(property);
				mProperties.put(slot, slotList);
			}

			if (property.hasOnSpawn()) {
				mSpawnedProperties.add(property);
			}
		}
	}

	private static int[] _getItems(PlayerInventory inv, ItemSlot slot) {
		switch (slot) {
		case MAINHAND:
			return new int[] {inv.getHeldItemSlot()};
		case OFFHAND:
			return new int[] {40};
		case ARMOR:
			return new int[] {36, 37, 38, 39};
		case NONE:
			return null;
		case INVENTORY:
			int[] indexes = new int[41];
			for (int i = 0; i <= 40; i++) {
				indexes[i] = i;
			}
			return indexes;
		}

		return null;
	}

	/*
	 * Updates the custom enchants for only the specific slot
	 */
	public void updateItemProperties(int index, Map<BaseEnchantment, Integer>propertyMap, Map<Integer, Map<BaseEnchantment, Integer>>inventoryMap, Player player, Plugin plugin) {
		try {

			final PlayerInventory inv = player.getInventory();
			ItemStack item = inv.getItem(index);
			Map<BaseEnchantment, Integer> invMapSlot = inventoryMap.get(index);

			//Sets ItemSlot enchants to check depending on what slot it is in inventory
			ItemSlot slot = null;
			if (index == player.getInventory().getHeldItemSlot()) {
				slot = ItemSlot.MAINHAND;
			} else if (index == 40) {
				slot = ItemSlot.OFFHAND;
			} else if (index >= 36 && index <= 39) {
				slot = ItemSlot.ARMOR;
			}

			//If map exists previously in slot, remove however much that slot had from it
			if (invMapSlot != null && !invMapSlot.isEmpty()) {
				//Removes the previously stored custom enchants from that item slot and applies the new enchant
				for (Map.Entry<BaseEnchantment, Integer> enchant : invMapSlot.entrySet()) {
					int newLevel = propertyMap.get(enchant.getKey()) - enchant.getValue();
					propertyMap.remove(enchant.getKey());
					enchant.getKey().removeProperty(plugin, player);
					if (newLevel > 0 || (enchant.getKey().negativeLevelsAllowed() && newLevel < 0)) {
						propertyMap.put(enchant.getKey(), newLevel);
						enchant.getKey().applyProperty(plugin, player, newLevel);
					}
				}

				//Clears stored custom enchants at that index in inventoryMap
				inventoryMap.remove(index);
				inventoryMap.put(index, new HashMap<BaseEnchantment, Integer>());
			}

			//If enum matches a slot (hand/armor), runs through those enchantments
			if (slot != null) {
				//Adds in the new custom enchants at the index only
				for (BaseEnchantment property : mProperties.get(slot)) {
					_updateItem(plugin, index, slot, item, player, property, propertyMap, inventoryMap);
				}
			}

			slot = ItemSlot.INVENTORY;
			//Checks for inventory enchants too, since all slots use them, so always checks regardless of above
			for (BaseEnchantment property : mProperties.get(slot)) {
				_updateItem(plugin, index, slot, item, player, property, propertyMap, inventoryMap);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void _updateItem(Plugin plugin, int index, ItemSlot slot, ItemStack item,
	                                Player player, BaseEnchantment property,
	                                Map<BaseEnchantment, Integer>propertyMap,
	                                Map<Integer, Map<BaseEnchantment, Integer>>inventoryMap) {
		if ((slot == ItemSlot.OFFHAND || slot == ItemSlot.MAINHAND) && item != null && ItemUtils.isWearable(item.getType())) {
			// Prevents armor items being held in mainhand / offhand counting towards enchantment level
			return;
		}

		int level = property.getLevelFromItem(item, player, slot);
		if (level > 0 || (property.negativeLevelsAllowed() && level < 0)) {
			Integer currentLevel = propertyMap.get(property);
			if (currentLevel != null) {
				currentLevel += level;
			} else {
				currentLevel = level;
			}
			propertyMap.put(property, currentLevel);
			inventoryMap.get(index).put(property, level);
			property.applyProperty(plugin, player, currentLevel);
		}
	}

	/*
	 * Build a map containing all of the ItemProperty's that apply to a player and the
	 * cumulative level of each property
	 *
	 * propertyMap is passed in by reference and updated by this function
	 *
	 * NOTE: propertyMap should already be clear'ed and the properties removed from the player
	 */
	public void getItemProperties(Map<BaseEnchantment, Integer>propertyMap, Map<Integer, Map<BaseEnchantment, Integer>>inventoryMap, Player player) {
		final PlayerInventory inv = player.getInventory();

		/* Step over the slots for which we have item properties */
		for (Map.Entry<ItemSlot, List<BaseEnchantment>> entry : mProperties.entrySet()) {
			ItemSlot slot = entry.getKey();
			List<BaseEnchantment> slotProperties = entry.getValue();

			if (slotProperties != null && !(slotProperties.isEmpty())) {
				/* Step over the item(s) the player has in that slot */
				int indexes[] = _getItems(inv, slot);
				if (indexes == null) {
					continue;
				}
				for (int index : indexes) {
					ItemStack item = inv.getItem(index);
					if (item != null) {
						/* Step over the properties that apply to that slot */
						for (BaseEnchantment property : slotProperties) {
							if ((slot == ItemSlot.OFFHAND || slot == ItemSlot.MAINHAND) && ItemUtils.isWearable(item.getType())) {
								// Prevents armor items being held in mainhand / offhand counting towards enchantment level
								continue;
							}

							int level = property.getLevelFromItem(item, player, slot);
							if (level > 0 || (property.negativeLevelsAllowed() && level < 0)) {
								Integer currentLevel = propertyMap.get(property);
								if (currentLevel != null) {
									currentLevel += level;
								} else {
									currentLevel = level;
								}
								propertyMap.put(property, currentLevel);
								inventoryMap.get(index).put(property, level);
								//Adds it to the inventoryMap's specific index for the custom enchant too
							}
						}
					}
				}
			}
		}
	}

	private void checkSpawnedItem(Item item) {
		if (item != null) {
			ItemStack stack = item.getItemStack();
			if (stack != null) {
				for (BaseEnchantment property : mSpawnedProperties) {
					int level = property.getLevelFromItem(stack);
					if (level > 0 || (property.negativeLevelsAllowed() && level < 0)) {
						property.onSpawn(mPlugin, item, level);
					}
				}
			}
		}
	}

	/*
	 * Watch for spawned items
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void itemSpawnEvent(ItemSpawnEvent event) {
		checkSpawnedItem(event.getEntity());
	}

	/*
	 * Chunk loading an item entity also counts as spawning
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void ChunkLoadEvent(ChunkLoadEvent event) {
		for (Entity entity : event.getChunk().getEntities()) {
			if (entity instanceof Item) {
				checkSpawnedItem((Item)entity);
			}
		}
	}
}

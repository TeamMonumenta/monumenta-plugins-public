package com.playmonumenta.plugins.enchantments;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions.AlchemistPotionsDamageEnchantment;
import com.playmonumenta.plugins.abilities.alchemist.EnfeeblingElixir.EnfeeblingElixirCooldownEnchantment;
import com.playmonumenta.plugins.abilities.alchemist.UnstableArrows.UnstableArrowsCooldownEnchantment;
import com.playmonumenta.plugins.abilities.alchemist.UnstableArrows.UnstableArrowsFuseEnchantment;
import com.playmonumenta.plugins.abilities.cleric.CelestialBlessing.CelestialCooldownEnchantment;
import com.playmonumenta.plugins.abilities.cleric.CleansingRain.CleansingRainCooldownEnchantment;
import com.playmonumenta.plugins.abilities.cleric.DivineJustice.DivineJusticeAllyHealingEnchantment;
import com.playmonumenta.plugins.abilities.cleric.SanctifiedArmor.SanctifiedArmorDamageEnchantment;
import com.playmonumenta.plugins.abilities.mage.FrostNova.FrostNovaCooldownEnchantment;
import com.playmonumenta.plugins.abilities.mage.MagmaShield.MagmaShieldCooldownEnchantment;
import com.playmonumenta.plugins.abilities.mage.ManaLance.ManaLanceCooldownEnchantment;
import com.playmonumenta.plugins.abilities.mage.Spellshock.SpellshockDamageEnchantment;
import com.playmonumenta.plugins.abilities.mage.ThunderStep.ThunderStepCooldownEnchantment;
import com.playmonumenta.plugins.abilities.rogue.AdvancingShadows.AdvancingShadowsCooldownEnchantment;
import com.playmonumenta.plugins.abilities.rogue.ByMyBlade.ByMyBladeCooldownEnchantment;
import com.playmonumenta.plugins.abilities.rogue.ByMyBlade.ByMyBladeDamageEnchantment;
import com.playmonumenta.plugins.abilities.rogue.DaggerThrow.DaggerThrowCooldownEnchantment;
import com.playmonumenta.plugins.abilities.rogue.Dodging.DodgingCooldownEnchantment;
import com.playmonumenta.plugins.abilities.scout.EagleEye.EagleEyeKillRefreshEnchantment;
import com.playmonumenta.plugins.abilities.scout.Volley.VolleyCooldownEnchantment;
import com.playmonumenta.plugins.abilities.warlock.AmplifyingHex.AmplifyingHexCooldownEnchantment;
import com.playmonumenta.plugins.abilities.warlock.AmplifyingHex.AmplifyingHexDamageEnchantment;
import com.playmonumenta.plugins.abilities.warlock.CholericFlames.CholericFlamesCooldownEnchantment;
import com.playmonumenta.plugins.abilities.warlock.CursedWound.CursedWoundDamageEnchantment;
import com.playmonumenta.plugins.abilities.warlock.PhlegmaticResolve.PhlegmaticResolveDefenseEnchantment;
import com.playmonumenta.plugins.abilities.warrior.BruteForce.BruteForceDamageEnchantment;
import com.playmonumenta.plugins.abilities.warrior.DefensiveLine.DefensiveLineCooldownEnchantment;
import com.playmonumenta.plugins.abilities.warrior.Riposte.RiposteCooldownEnchantment;
import com.playmonumenta.plugins.abilities.warrior.ShieldBash.ShieldBashCooldownEnchantment;
import com.playmonumenta.plugins.abilities.warrior.ShieldBash.ShieldBashDamageEnchantment;
import com.playmonumenta.plugins.enchantments.abilities.SpellPower;
import com.playmonumenta.plugins.enchantments.curses.CurseOfAnemia;
import com.playmonumenta.plugins.enchantments.curses.CurseOfCorruption;
import com.playmonumenta.plugins.enchantments.curses.CurseOfCrippling;
import com.playmonumenta.plugins.enchantments.curses.CurseOfEphemerality;
import com.playmonumenta.plugins.enchantments.curses.CurseOfShrapnel;
import com.playmonumenta.plugins.enchantments.curses.Starvation;
import com.playmonumenta.plugins.enchantments.curses.TwoHanded;
import com.playmonumenta.plugins.enchantments.other.PestilenceTesseract;
import com.playmonumenta.plugins.server.properties.ServerProperties;
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
		ARMOR, // Does NOT include offhand!
		INVENTORY, // Includes everything, including armor, offhand, and hotbar
		NONE;
	}

	public static final List<BaseEnchantment> CROSS_REGION_PROPERTIES = Arrays.asList(
			new RegionScalingDamageDealt(),
			//Curses
			new Starvation(),
			new TwoHanded(),
			new CurseOfAnemia(),
			new CurseOfCorruption(),
			new CurseOfCrippling(),
			new CurseOfEphemerality(),
			new CurseOfShrapnel(),

			//Utility Enchants
			new Recoil(),
			new RageOfTheKeter(),
			new Retrieval(),
			new ProtectionOfDepths(),
			new Radiant()
	);

	/*
	 * Keep a map of which properties apply to which slots
	 * This can dramatically reduce the number of checks done when looking through player inventories.
	 * Most items only apply to armor or mainhand, and we don't need to check every single item in the
	 * player's inventory * against these properties every time the player's inventory changes
	 */
	private final Map<Class<? extends BaseEnchantment>, BaseEnchantment> mEnchantLocator = new HashMap<>();
	private final Map<ItemSlot, List<BaseEnchantment>> mProperties = new EnumMap<>(ItemSlot.class);
	private final List<BaseSpawnableItemEnchantment> mSpawnedProperties = new ArrayList<>();
	private final Plugin mPlugin;

	public EnchantmentManager(Plugin plugin) {
		mPlugin = plugin;
	}

	public void load() {
		mProperties.clear();
		mSpawnedProperties.clear();

		final List<BaseEnchantment> init = new ArrayList<>();

		for (CustomEnchantment e : CustomEnchantment.values()) {
			init.add(e.getEnchantment());
		}

		// Ability Enchantments
		//Mage
		//init.add(new ManaLanceDamageEnchantment());
		init.add(new ManaLanceCooldownEnchantment());
		init.add(new SpellshockDamageEnchantment());
		init.add(new SpellPower());
		//init.add(new SpellshockRadiusEnchantment());
		init.add(new FrostNovaCooldownEnchantment());
		init.add(new MagmaShieldCooldownEnchantment());
		init.add(new ThunderStepCooldownEnchantment());

		//Rogue
		//init.add(new AdvancingShadowsRadiusEnchantment());
		//init.add(new AdvancingShadowsKnockbackRadiusEnchantment());
		//init.add(new AdvancingShadowsKnockbackSpeedEnchantment());
		init.add(new AdvancingShadowsCooldownEnchantment());
		init.add(new ByMyBladeDamageEnchantment());
		init.add(new ByMyBladeCooldownEnchantment());
		//init.add(new ByMyBladeHasteEnchantment());
		//init.add(new ByMyBladeDurationEnchantment());
		init.add(new DodgingCooldownEnchantment());
		init.add(new DaggerThrowCooldownEnchantment());

		//Scout
		//init.add(new VolleyDamageEnchantment());
		//init.add(new VolleyArrowsEnchantment());
		init.add(new VolleyCooldownEnchantment());
		//init.add(new VolleyMultiplierEnchantment());
		init.add(new EagleEyeKillRefreshEnchantment());

		//Warrior
		init.add(new BruteForceDamageEnchantment());
		//init.add(new BruteForceRadiusEnchantment());
		//init.add(new BruteForceKnockbackEnchantment());
		init.add(new ShieldBashCooldownEnchantment());
		init.add(new ShieldBashDamageEnchantment());
		init.add(new DefensiveLineCooldownEnchantment());
		init.add(new RiposteCooldownEnchantment());

		//Cleric
		init.add(new CelestialCooldownEnchantment());
		init.add(new CleansingRainCooldownEnchantment());
		//init.add(new CleansingRainRadiusEnchantment());
		init.add(new SanctifiedArmorDamageEnchantment());
		init.add(new DivineJusticeAllyHealingEnchantment());

		//Alchemist
		//init.add(new IronTinctureAbsorptionEnchantment());
		//init.add(new IronTinctureCooldownEnchantment());
		init.add(new AlchemistPotionsDamageEnchantment());
		init.add(new EnfeeblingElixirCooldownEnchantment());
		init.add(new UnstableArrowsCooldownEnchantment());
		init.add(new UnstableArrowsFuseEnchantment());

		//Warlock
		init.add(new AmplifyingHexDamageEnchantment());
		init.add(new AmplifyingHexCooldownEnchantment());
		init.add(new CholericFlamesCooldownEnchantment());
		init.add(new PhlegmaticResolveDefenseEnchantment());
		init.add(new CursedWoundDamageEnchantment());


		// Tesseracts (not actually items a player can get enchants from)
		init.add(new PestilenceTesseract());

		mEnchantLocator.clear();

		/* Build the map of which slots have which properties */
		for (BaseEnchantment property : init) {
			mEnchantLocator.put(property.getClass(), property);
			for (ItemSlot slot : property.getValidSlots()) {
				List<BaseEnchantment> slotList = mProperties.get(slot);
				if (slotList == null) {
					slotList = new ArrayList<BaseEnchantment>();
				}
				slotList.add(property);
				mProperties.put(slot, slotList);
			}

			if (property instanceof BaseSpawnableItemEnchantment) {
				mSpawnedProperties.add((BaseSpawnableItemEnchantment)property);
			}
		}
	}

	private static int[] getItems(PlayerInventory inv, ItemSlot slot) {
		switch (slot) {
		case MAINHAND:
			return new int[] {inv.getHeldItemSlot()};
		case OFFHAND:
			return new int[] {40};
		case ARMOR:
			return new int[] {36, 37, 38, 39};
		case INVENTORY:
			int[] indexes = new int[41];
			for (int i = 0; i <= 40; i++) {
				indexes[i] = i;
			}
			return indexes;
		case NONE:
		default:
			return null;
		}
	}

	public BaseEnchantment getEnchantmentHandle(Class<? extends BaseEnchantment> cls) {
		return mEnchantLocator.get(cls);
	}

	/*
	 * Updates the custom enchants for only the specific slot
	 */
	public void updateItemProperties(int index, Map<BaseEnchantment, Integer> propertyMap, Map<Integer, Map<BaseEnchantment, Integer>> inventoryMap, Player player, Plugin plugin) {
		try {
			if (index == -999) {
				return;
			}

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
					if (newLevel > 0 || (enchant.getKey().canNegativeLevel() && newLevel < 0)) {
						propertyMap.put(enchant.getKey(), newLevel);
						enchant.getKey().applyProperty(plugin, player, newLevel);
					}
				}

				//Clears stored custom enchants at that index in inventoryMap
				inventoryMap.remove(index);
				inventoryMap.put(index, new HashMap<>());
			}

			//If enum matches a slot (hand/armor), runs through those enchantments
			if (slot != null) {
				updateItem(plugin, index, slot, item, player, mProperties.get(slot), propertyMap, inventoryMap);
			}

			// Checks for inventory enchants too, since all slots use them, so always checks regardless of above
			updateItem(plugin, index, ItemSlot.INVENTORY, item, player, mProperties.get(ItemSlot.INVENTORY), propertyMap, inventoryMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void updateItem(Plugin plugin, int index, ItemSlot slot, ItemStack item, Player player, List<BaseEnchantment> properties,
			Map<BaseEnchantment, Integer> propertyMap, Map<Integer, Map<BaseEnchantment, Integer>> inventoryMap) {
		// Prevent mainhand custom enchants from registering for R2 items in R1
		RegionScalingDamageDealt regionScaling = new RegionScalingDamageDealt();
		if (slot == ItemSlot.MAINHAND && !ServerProperties.getClassSpecializationsEnabled()
				&& regionScaling.getPlayerItemLevel(item, player, slot) > 0) {
			for (BaseEnchantment property : CROSS_REGION_PROPERTIES) {
				updateItem(plugin, index, slot, item, player, property, propertyMap, inventoryMap);
			}

			return;
		}

		// Prevents armor items being held in mainhand / offhand counting towards enchantment level
		if ((slot == ItemSlot.OFFHAND || slot == ItemSlot.MAINHAND) && item != null && ItemUtils.isWearable(item)) {
			return;
		}

		for (BaseEnchantment property : properties) {
			updateItem(plugin, index, slot, item, player, property, propertyMap, inventoryMap);
		}
	}

	private static void updateItem(Plugin plugin, int index, ItemSlot slot, ItemStack item,
	                               Player player, BaseEnchantment property,
	                               Map<BaseEnchantment, Integer> propertyMap,
	                               Map<Integer, Map<BaseEnchantment, Integer>> inventoryMap) {
		int level = property.getPlayerItemLevel(item, player, slot);
		if (level > 0 || (property.canNegativeLevel() && level < 0)) {
			Integer currentLevel = propertyMap.get(property);
			if (currentLevel != null) {
				currentLevel += level;
			} else {
				currentLevel = level;
			}
			propertyMap.put(property, currentLevel);
			Map<BaseEnchantment, Integer> getMapIndex = inventoryMap.get(index);
			if (getMapIndex != null) {
				getMapIndex.put(property, level);
			}
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
	public void getItemProperties(Map<BaseEnchantment, Integer> propertyMap, Map<Integer, Map<BaseEnchantment, Integer>> inventoryMap, Player player) {
		final PlayerInventory inv = player.getInventory();

		/* Step over the slots for which we have item properties */
		for (Map.Entry<ItemSlot, List<BaseEnchantment>> entry : mProperties.entrySet()) {
			ItemSlot slot = entry.getKey();
			List<BaseEnchantment> slotProperties = entry.getValue();

			if (slotProperties != null && !(slotProperties.isEmpty())) {
				/* Step over the item(s) the player has in that slot */
				int[] indexes = getItems(inv, slot);
				if (indexes == null) {
					continue;
				}
				for (int index : indexes) {
					ItemStack item = inv.getItem(index);
					if (item != null) {
						/* Step over the properties that apply to that slot */
						for (BaseEnchantment property : slotProperties) {
							if ((slot == ItemSlot.OFFHAND || slot == ItemSlot.MAINHAND) && ItemUtils.isWearable(item)) {
								// Prevents armor items being held in mainhand / offhand counting towards enchantment level
								continue;
							}

							int level = property.getPlayerItemLevel(item, player, slot);
							if (level > 0 || (property.canNegativeLevel() && level < 0)) {
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
				for (BaseSpawnableItemEnchantment property : mSpawnedProperties) {
					int level = property.getItemLevel(stack);
					if (level > 0 || (property.canNegativeLevel() && level < 0)) {
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
	public void chunkLoadEvent(ChunkLoadEvent event) {
		for (Entity entity : event.getChunk().getEntities()) {
			if (entity instanceof Item) {
				checkSpawnedItem((Item)entity);
			}
		}
	}
}

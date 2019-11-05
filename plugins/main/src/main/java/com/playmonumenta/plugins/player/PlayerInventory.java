package com.playmonumenta.plugins.player;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.nms.utils.NmsCommandUtils;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.events.BossAbilityDamageEvent;
import com.playmonumenta.plugins.events.EvasionEvent;
import com.playmonumenta.plugins.listeners.ShulkerEquipmentListener;

public class PlayerInventory {
	private Set<Material> mNoOffhandFunctionMats = EnumSet.noneOf(Material.class);
	private Set<Material> mNoOffhandRemoveFunctionMats = EnumSet.noneOf(Material.class);
	private Map<Material, NmsCommandUtils.ParsedCommandWrapper> mOffhandFunctions = new EnumMap<>(Material.class);
	private Map<Material, NmsCommandUtils.ParsedCommandWrapper> mOffhandRemoveFunctions = new EnumMap<>(Material.class);

	/*
	 * This list contains all of a player's currently valid item properties,
	 * including ones that are on duplicate specialized lists below
	 * Contains the hashmaps in a hashmap that represents the inventory slots
	 *
	 * mInventoryProperties indexes 0-40 for inventory slots and custom enchants.
	 * 0-8 = hotbar, 36-39 = armor, 40 = offhand
	 */

	private Map<Integer, Map<BaseEnchantment, Integer>> mInventoryProperties = new HashMap<Integer, Map<BaseEnchantment, Integer>>();
	private Map<BaseEnchantment, Integer> mCurrentProperties = new HashMap<BaseEnchantment, Integer>();
	private Map<BaseEnchantment, Integer> mPreviousProperties = new HashMap<BaseEnchantment, Integer>();

	//Set true when player shift clicks items in inventory so it only runs after inventory is closed
	private boolean hasShiftClicked = false;

	private Material mPrevOffhandMat = null;
	private List<String> mPrevOffhandLore = null;

	public PlayerInventory(Plugin plugin, Player player) {
		updateEquipmentProperties(plugin, player, null);
	}

	public void tick(Plugin plugin, World world, Player player) {
		// Players in spectator do not have ticking effects
		// TODO: Add vanish hook here also
		if (player.getGameMode().equals(GameMode.SPECTATOR)) {
			return;
		}

		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.tick(plugin, world, player, level);
		}
	}

	private static void runOffhandFunction(Plugin plugin, String functionFolder, Material type,
	                                       Set<Material>noFunctionSet,
	                                       Map<Material, NmsCommandUtils.ParsedCommandWrapper>functionMap,
	                                       Player player) {
		if (type != null && !noFunctionSet.contains(type)) {
			// This particular material either hasn't been tested yet or it has a corresponding function

			if (!functionMap.containsKey(type)) {
				// Don't have this function cached - need to see if it exists and cache it
				String cmd = "function " + functionFolder + "/" + type.toString().toLowerCase();
				try {
					plugin.getLogger().info("Parsing command: '" + cmd + "'");
					functionMap.put(type, NmsCommandUtils.parseCommand(cmd));
				} catch (Exception e) {
					plugin.getLogger().info("Failed to parse buyback command '" + cmd + "' : " + e.getMessage());

					// This function doesn't exist - mark it as such and never try again
					noFunctionSet.add(type);
				}
			}

			NmsCommandUtils.ParsedCommandWrapper cmd = functionMap.get(type);
			if (cmd != null) {
				try {
					NmsCommandUtils.runParsedCommand(cmd, player);
				} catch (Exception e) {
					plugin.getLogger().warning("Failed to run cached offhand command: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	public void updateEquipmentProperties(Plugin plugin, Player player, Event event) {
		//Runs offhand check for things like crystalizers
		if (event == null || event instanceof PlayerSwapHandItemsEvent || event instanceof InventoryClickEvent) {
			// Offhand item change detection if the parsed command worked
			ItemStack offhand = player.getInventory().getItemInOffHand();

			Material type = null;
			if (offhand != null) {
				type = offhand.getType();
			}

			if (type != mPrevOffhandMat || (offhand.hasItemMeta() && offhand.getItemMeta().hasLore() && !offhand.getItemMeta().getLore().equals(mPrevOffhandLore))) {
				runOffhandFunction(plugin, "monumenta:on_offhand_remove", mPrevOffhandMat, mNoOffhandRemoveFunctionMats, mOffhandRemoveFunctions, player);

				mPrevOffhandMat = type;
				mPrevOffhandLore = offhand.hasItemMeta() && offhand.getItemMeta().hasLore() ? offhand.getItemMeta().getLore() : null;

				runOffhandFunction(plugin, "monumenta:on_offhand", type, mNoOffhandFunctionMats, mOffhandFunctions, player);
			}
		}

		//Updates different indexes for custom enchant depending on the event given, if null or not listed, rescan everything
		if (event instanceof InventoryClickEvent) {
			if (((InventoryClickEvent) event).isShiftClick()) {
				hasShiftClicked = true;
				return;
			} else if (((InventoryClickEvent) event).isRightClick() && ShulkerEquipmentListener.isEquipmentBox(((InventoryClickEvent) event).getCurrentItem()))  {
				for (int i = 0; i <= 8; i++) {
					// Update hotbar
					plugin.mEnchantmentManager.updateItemProperties(i, mCurrentProperties, mInventoryProperties, player, plugin);
				}
				for (int i = 36; i <= 40; i++) {
					// Update armor and offhand
					plugin.mEnchantmentManager.updateItemProperties(i, mCurrentProperties, mInventoryProperties, player, plugin);
				}
			} else if (((InventoryClickEvent) event).getHotbarButton() != -1) {
				// Updates clicked slot and hotbar slot if numbers were used to swap
				plugin.mEnchantmentManager.updateItemProperties(((InventoryClickEvent) event).getSlot(), mCurrentProperties, mInventoryProperties, player, plugin);
				plugin.mEnchantmentManager.updateItemProperties(((InventoryClickEvent) event).getHotbarButton(), mCurrentProperties, mInventoryProperties, player, plugin);
			} else {
				plugin.mEnchantmentManager.updateItemProperties(((InventoryClickEvent) event).getSlot(), mCurrentProperties, mInventoryProperties, player, plugin);
			}
		} else if (event instanceof InventoryDragEvent) {
			for (int i : ((InventoryDragEvent) event).getInventorySlots()) {
				plugin.mEnchantmentManager.updateItemProperties(i, mCurrentProperties, mInventoryProperties, player, plugin);
			}
		} else if (event instanceof PlayerInteractEvent || event instanceof BlockDispenseArmorEvent) {
			plugin.mEnchantmentManager.updateItemProperties(36, mCurrentProperties, mInventoryProperties, player, plugin);
			plugin.mEnchantmentManager.updateItemProperties(37, mCurrentProperties, mInventoryProperties, player, plugin);
			plugin.mEnchantmentManager.updateItemProperties(38, mCurrentProperties, mInventoryProperties, player, plugin);
			plugin.mEnchantmentManager.updateItemProperties(39, mCurrentProperties, mInventoryProperties, player, plugin);
		} else if (event instanceof PlayerItemBreakEvent) {
			//Updates item properties for armor, mainhand and offhand
			plugin.mEnchantmentManager.updateItemProperties(player.getInventory().getHeldItemSlot(), mCurrentProperties, mInventoryProperties, player, plugin);
			plugin.mEnchantmentManager.updateItemProperties(36, mCurrentProperties, mInventoryProperties, player, plugin);
			plugin.mEnchantmentManager.updateItemProperties(37, mCurrentProperties, mInventoryProperties, player, plugin);
			plugin.mEnchantmentManager.updateItemProperties(38, mCurrentProperties, mInventoryProperties, player, plugin);
			plugin.mEnchantmentManager.updateItemProperties(39, mCurrentProperties, mInventoryProperties, player, plugin);
			plugin.mEnchantmentManager.updateItemProperties(40, mCurrentProperties, mInventoryProperties, player, plugin);
		} else if (event instanceof PlayerItemHeldEvent) {
			plugin.mEnchantmentManager.updateItemProperties(((PlayerItemHeldEvent) event).getPreviousSlot(), mCurrentProperties, mInventoryProperties, player, plugin);
			plugin.mEnchantmentManager.updateItemProperties(((PlayerItemHeldEvent) event).getNewSlot(), mCurrentProperties, mInventoryProperties, player, plugin);
		} else if (event instanceof PlayerSwapHandItemsEvent) {
			plugin.mEnchantmentManager.updateItemProperties(player.getInventory().getHeldItemSlot(), mCurrentProperties, mInventoryProperties, player, plugin);
			plugin.mEnchantmentManager.updateItemProperties(40, mCurrentProperties, mInventoryProperties, player, plugin);
		} else if (event instanceof PlayerDropItemEvent) {
			plugin.mEnchantmentManager.updateItemProperties(player.getInventory().getHeldItemSlot(), mCurrentProperties, mInventoryProperties, player, plugin);
		} else if (!hasShiftClicked && event instanceof InventoryCloseEvent) {
			return; //Only ever updates on InventoryCloseEvent if shift clicks have been made
		} else {

			if (hasShiftClicked && event instanceof InventoryCloseEvent) {
				hasShiftClicked = false;
			}//Sets hasShiftClicked to false after updating entire inventory

			// Swap current and previous lists
			mPreviousProperties = new HashMap<BaseEnchantment, Integer>();
			Map<BaseEnchantment, Integer> temp = mPreviousProperties;
			mPreviousProperties = mCurrentProperties;
			mCurrentProperties = temp;

			for (int i = 0; i <= 40; i++) {
				mInventoryProperties.put(i, new HashMap<BaseEnchantment, Integer>());
			}

			// Clear the current map and update it with current properties
			mCurrentProperties.clear();
			try {
				plugin.mEnchantmentManager.getItemProperties(mCurrentProperties, mInventoryProperties, player);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Remove properties from the player that were removed
			for (BaseEnchantment property : mPreviousProperties.keySet()) {
				if (!mCurrentProperties.containsKey(property)) {
					property.removeProperty(plugin, player);
				}
			}

			// Apply properties to the player that changed or have a new level
			for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
				BaseEnchantment property = iter.getKey();
				Integer level = iter.getValue();

				Integer oldLevel = mPreviousProperties.get(property);
				if (oldLevel == level) {
					// Don't need to do anything - player already had effects from this
				} else if (oldLevel == null) {
					// This didn't exist before - just apply the new property
					property.applyProperty(plugin, player, level);
				} else {
					// This existed before but was a different level - clear and re-add
					property.removeProperty(plugin, player);
					property.applyProperty(plugin, player, level);
				}
			}
		}

		//Runs onEquipmentUpdate() method for all BaseEnchantments
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();

			property.onEquipmentUpdate(plugin, player);
		}
	}

	public void onKill(Plugin plugin, Player player, Entity target, EntityDeathEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onKill(plugin, player, level, target, event);
		}
	}

	public void onAttack(Plugin plugin, Player player, LivingEntity target, EntityDamageByEntityEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onAttack(plugin, player, level, target, event);
		}
	}

	public void onDamage(Plugin plugin, Player player, LivingEntity target, EntityDamageByEntityEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onDamage(plugin, player, level, target, event);
		}
	}

	public void onLaunchProjectile(Plugin plugin, Player player, Projectile proj, ProjectileLaunchEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onLaunchProjectile(plugin, player, level, proj, event);
		}
	}

	public void onExpChange(Plugin plugin, Player player, PlayerExpChangeEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onExpChange(plugin, player, event, level);
		}
	}

	public void onBlockBreak(Plugin plugin, Player player, BlockBreakEvent event, ItemStack item) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onBlockBreak(plugin, player, event, item, level);
		}
	}

	public void onPlayerInteract(Plugin plugin, Player player, PlayerInteractEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onPlayerInteract(plugin, player, event, level);
		}
	}

	public void onDeath(Plugin plugin, Player player, PlayerDeathEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onDeath(plugin, player, event, level);
		}
	}

	public void onHurt(Plugin plugin, Player player, EntityDamageEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onHurt(plugin, player, level, event);
		}
	}

	public void onBossDamage(Plugin plugin, Player player, BossAbilityDamageEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onBossDamage(plugin, player, level, event);
		}
	}


	public void onHurtByEntity(Plugin plugin, Player player, EntityDamageByEntityEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onHurtByEntity(plugin, player, level, event);
		}
	}

	public void onEvade(Plugin plugin, Player player, EvasionEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onEvade(plugin, player, level, event);
		}
	}

	public void onConsume(Plugin plugin, Player player, PlayerItemConsumeEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();
			property.onConsume(plugin, player, event, level);
		}
	}

	public void removeProperties(Plugin plugin, Player player) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();

			property.removeProperty(plugin, player);
		}

		mCurrentProperties.clear();
	}
}

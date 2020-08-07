package com.playmonumenta.plugins.player;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.GameMode;
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
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseAttribute;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.Enchantment;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.events.EvasionEvent;
import com.playmonumenta.plugins.listeners.ShulkerEquipmentListener;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class PlayerInventory {
	/*
	 * This list contains all of a player's currently valid item properties,
	 * including ones that are on duplicate specialized lists below
	 * Contains the hashmaps in a hashmap that represents the inventory slots
	 *
	 * mInventoryProperties indexes 0-40 for inventory slots and custom enchants.
	 * 0-8 = hotbar, 36-39 = armor, 40 = offhand
	 *
	 * Needs to be ordered so that trigger orders are correct
	 */

	private Map<Integer, Map<BaseEnchantment, Integer>> mInventoryProperties = new LinkedHashMap<Integer, Map<BaseEnchantment, Integer>>();
	private Map<BaseEnchantment, Integer> mCurrentProperties = new LinkedHashMap<BaseEnchantment, Integer>();
	private Map<BaseEnchantment, Integer> mPreviousProperties = new LinkedHashMap<BaseEnchantment, Integer>();

	//Set true when player shift clicks items in inventory so it only runs after inventory is closed
	private boolean mNeedsUpdate = false;

	public PlayerInventory(Plugin plugin, Player player) {
		InventoryUtils.scheduleDelayedEquipmentCheck(plugin, player, new PlayerJoinEvent(player, ""));	// Just a dummy event
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

	//Updates only for the slot given
	public void updateItemSlotProperties(Plugin plugin, Player player, int slot) {
		plugin.mEnchantmentManager.updateItemProperties(slot, mCurrentProperties, mInventoryProperties, player, plugin);
	}

	public void updateEquipmentProperties(Plugin plugin, Player player, Event event) {
		// If the player transferred shards (join event), clear all properties and re-apply the relevant ones
		if (event instanceof PlayerJoinEvent) {
			for (Enchantment e : Enchantment.values()) {
				if (e.isCustomEnchant()) {
					e.getEnchantClass().removeProperty(plugin, player);
				}
			}

			for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
				BaseEnchantment property = iter.getKey();
				Integer level = iter.getValue();
				property.applyProperty(plugin, player, level);
			}
		}

		// Updates different indexes for custom enchant depending on the event given, if null or not listed, rescan everything
		if (event instanceof InventoryClickEvent) {
			InventoryClickEvent invClickEvent = (InventoryClickEvent) event;
			if (invClickEvent.getSlotType() == InventoryType.SlotType.CRAFTING
				|| invClickEvent.isShiftClick() || invClickEvent.getSlot() == -1) {
				mNeedsUpdate = true;
				return;
			} else if (invClickEvent.isRightClick() && ShulkerEquipmentListener.isEquipmentBox(invClickEvent.getCurrentItem()))  {
				for (int i = 0; i <= 8; i++) {
					// Update hotbar
					plugin.mEnchantmentManager.updateItemProperties(i, mCurrentProperties, mInventoryProperties, player, plugin);
				}
				for (int i = 36; i <= 40; i++) {
					// Update armor and offhand
					plugin.mEnchantmentManager.updateItemProperties(i, mCurrentProperties, mInventoryProperties, player, plugin);
				}
			} else if (invClickEvent.getHotbarButton() != -1) {
				// Updates clicked slot and hotbar slot if numbers were used to swap
				plugin.mEnchantmentManager.updateItemProperties(invClickEvent.getSlot(), mCurrentProperties, mInventoryProperties, player, plugin);
				plugin.mEnchantmentManager.updateItemProperties(invClickEvent.getHotbarButton(), mCurrentProperties, mInventoryProperties, player, plugin);
			} else {
				plugin.mEnchantmentManager.updateItemProperties(invClickEvent.getSlot(), mCurrentProperties, mInventoryProperties, player, plugin);
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
		} else if (!mNeedsUpdate && event instanceof InventoryCloseEvent) {
			return; //Only ever updates on InventoryCloseEvent if shift clicks have been made
		} else {

			// Sets mHasShiftClicked to false after updating entire inventory
			if (mNeedsUpdate && event instanceof InventoryCloseEvent) {
				mNeedsUpdate = false;
			}

			// Swap current and previous lists
			mPreviousProperties = new LinkedHashMap<BaseEnchantment, Integer>();
			Map<BaseEnchantment, Integer> temp = mPreviousProperties;
			mPreviousProperties = mCurrentProperties;
			mCurrentProperties = temp;

			for (int i = 0; i <= 40; i++) {
				mInventoryProperties.put(i, new LinkedHashMap<BaseEnchantment, Integer>());
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

		// Attributes
		// Since they're parsed more efficiently than enchants (and this is temporary anyways), not worried about maximizing efficiency of every case
		if (event instanceof PlayerItemHeldEvent || event instanceof PlayerDropItemEvent) {
			plugin.mAttributeManager.updateAttributeTrie(plugin, player, true);
		} else {
			plugin.mAttributeManager.updateAttributeTrie(plugin, player, false);
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

	public void onAbility(Plugin plugin, Player player, LivingEntity target, CustomDamageEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onAbility(plugin, player, level, target, event);
		}
	}

	public void onLaunchProjectile(Plugin plugin, Player player, Projectile proj, ProjectileLaunchEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onLaunchProjectile(plugin, player, level, proj, event);
		}

		for (BaseAttribute attribute : plugin.mAttributeManager.mAttributes) {
			attribute.onLaunchProjectile(plugin, player, plugin.mAttributeManager.mAttributeTrie.get(attribute.getProperty(), player), proj, event);
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

	public void onHurtByEntity(Plugin plugin, Player player, EntityDamageByEntityEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onHurtByEntity(plugin, player, level, event);
		}
	}

	public void onFatalHurt(Plugin plugin, Player player, EntityDamageEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			// It is necessary to check each time in case the previous call to onFatalHurt() made the damage not fatal
			if (player.getHealth() + AbsorptionUtils.getAbsorption(player) - EntityUtils.getRealFinalDamage(event) <= 0) {
				property.onFatalHurt(plugin, player, level, event);
			}
		}
	}

	public void onRegain(Plugin plugin, Player player, EntityRegainHealthEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();

			property.onRegain(plugin, player, level, event);
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

	public int getEnchantmentLevel(Plugin plugin, Class<? extends BaseEnchantment> cls) {
        BaseEnchantment enchant = plugin.mEnchantmentManager.getEnchantmentHandle(cls);
        if (enchant != null && mCurrentProperties != null) {
            Integer level = mCurrentProperties.get(enchant);
            if (level != null) {
                return level;
            }
        }
        return 0;
    }
}

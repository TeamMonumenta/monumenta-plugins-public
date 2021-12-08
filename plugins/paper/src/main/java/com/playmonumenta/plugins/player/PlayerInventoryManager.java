package com.playmonumenta.plugins.player;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.attributes.BaseAttribute;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.CustomEnchantment;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.events.EvasionEvent;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.listeners.ShulkerEquipmentListener;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

import net.kyori.adventure.text.Component;

public class PlayerInventoryManager {
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

	@NotNull private ItemStack[] mInventoryLastCheck = new ItemStack[41];
	private Map<Integer, Map<BaseEnchantment, Integer>> mInventoryProperties = new LinkedHashMap<>();
	private Map<BaseEnchantment, Integer> mCurrentProperties = new LinkedHashMap<>();
	private Map<BaseEnchantment, Integer> mPreviousProperties = new LinkedHashMap<>();

	//Set true when player shift clicks items in inventory so it only runs after inventory is closed
	private boolean mNeedsUpdate = false;

	public PlayerInventoryManager(Plugin plugin, Player player) {
		InventoryUtils.scheduleDelayedEquipmentCheck(plugin, player, new PlayerJoinEvent(player, Component.text(""))); // Just a dummy event
	}

	public void tick(Plugin plugin, Player player) {
		// Players in spectator or who are vanished do not have ticking effects
		if (PremiumVanishIntegration.isInvisible(player) || player.getGameMode().equals(GameMode.SPECTATOR)) {
			return;
		}

		for (
			@NotNull Map.Entry<@Nullable BaseEnchantment, @Nullable Integer> currentProperty
				: mCurrentProperties.entrySet()
		) {
			//TODO room for refactoring?
			// Below, args passed are Integer but param already is of type int (assumptions).
			// BaseEnchantment could have its methods static,
			// using .class to set ordering (similar to plans for null player Abilities),
			// while creating BaseEnchant objects with int (not Integer) properties to store data?
			@Nullable BaseEnchantment baseEnchantment = currentProperty.getKey();
			@Nullable Integer level = currentProperty.getValue();

			if (baseEnchantment != null) {
				baseEnchantment.tick(
					plugin,
					player,
					(level == null) ? 0 : level
				);
			}
		}
	}

	//Updates only for the slot given
	public void updateItemSlotProperties(@NotNull Plugin plugin, @NotNull Player player, int slot) {
		@NotNull ItemStack[] inv = player.getInventory().getContents();
		if (slot < 0 || slot >= inv.length) {
			return;
		}
		plugin.mEnchantmentManager.updateItemProperties(slot, mCurrentProperties, mInventoryProperties, player, plugin);
		updateItemLastCheck(slot, inv[slot]);
	}

	public void updateItemLastCheck(int slot, @Nullable ItemStack item) {
		if (item == null) {
			mInventoryLastCheck[slot] = null;
		} else {
			mInventoryLastCheck[slot] = item.clone();
		}
	}

	public void updateEquipmentProperties(Plugin plugin, Player player, Event event) {
		// If the player transferred shards (join event), clear most types of custom enchants and re-apply the relevant ones
		if (event instanceof PlayerJoinEvent) {
			for (CustomEnchantment e : CustomEnchantment.values()) {
				e.getEnchantment().removeProperty(plugin, player);
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
			} else if (invClickEvent.isRightClick() && ShulkerEquipmentListener.isEquipmentBox(invClickEvent.getCurrentItem())) {
				for (int i = 0; i <= 8; i++) {
					// Update hotbar
					updateItemSlotProperties(plugin, player, i);
				}
				for (int i = 36; i <= 40; i++) {
					// Update armor and offhand
					updateItemSlotProperties(plugin, player, i);
				}
			} else if (invClickEvent.getHotbarButton() != -1) {
				// Updates clicked slot and hotbar slot if numbers were used to swap
				updateItemSlotProperties(plugin, player, invClickEvent.getSlot());
				updateItemSlotProperties(plugin, player, invClickEvent.getHotbarButton());
			} else if (invClickEvent.getClick().equals(ClickType.SWAP_OFFHAND)) {
				// Updates clicked slot and offhand slot when swap hands key is used
				updateItemSlotProperties(plugin, player, invClickEvent.getSlot());
				updateItemSlotProperties(plugin, player, 40);
			} else {
				updateItemSlotProperties(plugin, player, invClickEvent.getSlot());
			}
		} else if (event instanceof InventoryDragEvent) {
			for (int i : ((InventoryDragEvent) event).getInventorySlots()) {
				updateItemSlotProperties(plugin, player, i);
			}
		} else if (event instanceof PlayerInteractEvent || event instanceof BlockDispenseArmorEvent) {
			for (int i = 36; i <= 39; i++) {
				// Update armor
				updateItemSlotProperties(plugin, player, i);
			}
		} else if (event instanceof PlayerItemBreakEvent) {
			//Updates item properties for armor, mainhand and offhand
			updateItemSlotProperties(plugin, player, player.getInventory().getHeldItemSlot());
			for (int i = 36; i <= 40; i++) {
				// Update armor and offhand
				updateItemSlotProperties(plugin, player, i);
			}
		} else if (event instanceof PlayerItemHeldEvent) {
			updateItemSlotProperties(plugin, player, ((PlayerItemHeldEvent) event).getPreviousSlot());
			updateItemSlotProperties(plugin, player, ((PlayerItemHeldEvent) event).getNewSlot());
		} else if (event instanceof PlayerSwapHandItemsEvent) {
			updateItemSlotProperties(plugin, player, player.getInventory().getHeldItemSlot());
			updateItemSlotProperties(plugin, player, 40);
		} else if (event instanceof PlayerDropItemEvent) {
			int heldItemSlot = player.getInventory().getHeldItemSlot();
			if (hasSlotChanged(player, heldItemSlot)) {
				updateItemSlotProperties(plugin, player, heldItemSlot);
			} else {
				int droppedSlot = getDroppedSlotId((PlayerDropItemEvent) event);
				updateItemSlotProperties(plugin, player, droppedSlot);
			}
		} else if (!mNeedsUpdate && event instanceof InventoryCloseEvent) {
			return; //Only ever updates on InventoryCloseEvent if shift clicks have been made
		} else {

			// Sets mHasShiftClicked to false after updating entire inventory
			if (mNeedsUpdate && event instanceof InventoryCloseEvent) {
				mNeedsUpdate = false;
			}

			ItemStack[] inv = player.getInventory().getContents();
			for (int i = 0; i <= 40; i++) {
				updateItemLastCheck(i, inv[i]);
				mInventoryProperties.put(i, new LinkedHashMap<>());
			}

			// Current properties becomes previous, update current properties
			mPreviousProperties = mCurrentProperties;
			mCurrentProperties = new LinkedHashMap<>();
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
				if (oldLevel == level && !(event instanceof EntityResurrectEvent)) {
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
		// Room for optimisation,
		// see TODO above BaseEnchantment#negativeLevelsAllowed
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

		for (BaseAttribute attribute : plugin.mAttributeManager.mAttributes) {
			attribute.onDamage(plugin, player, plugin.mAttributeManager.mAttributeTrie.get(attribute.getProperty(), player), event);
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

		for (BaseAttribute attribute : plugin.mAttributeManager.mAttributes) {
			attribute.onHurtByEntity(plugin, player, plugin.mAttributeManager.mAttributeTrie.get(attribute.getProperty(), player), event);
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

	public void onItemDamage(Plugin plugin, Player player, PlayerItemDamageEvent event) {
		for (Map.Entry<BaseEnchantment, Integer> iter : mCurrentProperties.entrySet()) {
			BaseEnchantment property = iter.getKey();
			Integer level = iter.getValue();
			property.onItemDamage(plugin, player, event, level);
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
		@Nullable BaseEnchantment enchant = plugin.mEnchantmentManager.getEnchantmentHandle(cls);
		if (enchant != null && mCurrentProperties != null) {
			@Nullable Integer level = mCurrentProperties.get(enchant);
			if (level != null) {
				return level;
			}
		}

		return 0;
	}

	public boolean hasSlotChanged(@NotNull Player player, int slot) {
		if (slot < 0 || slot > 40) {
			return false;
		}
		@Nullable ItemStack oldItem = mInventoryLastCheck[slot];
		@Nullable ItemStack currentItem = player.getInventory().getContents()[slot];
		if (oldItem == null) {
			return currentItem == null;
		} else {
			return !oldItem.equals(currentItem);
		}
	}

	// Returns the first similar slot's number where there is a difference in item count, or -1 if not found
	public int getDroppedSlotId(@NotNull PlayerDropItemEvent event) {
		@NotNull Player player = event.getPlayer();
		@NotNull ItemStack[] inv = player.getInventory().getContents();
		@NotNull ItemStack droppedItem = event.getItemDrop().getItemStack();

		for (int slot = 0; slot <= 40; slot++) {
			@Nullable ItemStack oldItem = mInventoryLastCheck[slot];
			if (!droppedItem.isSimilar(oldItem)) {
				continue;
			}
			int oldAmount = oldItem.getAmount();

			@Nullable ItemStack currentItem = inv[slot];
			if (droppedItem.isSimilar(currentItem)) {
				int newAmount = currentItem.getAmount();
				if (oldAmount - newAmount > 0) {
					return slot;
				}
			} else {
				return slot;
			}
		}
		return -1;
	}

	public JsonObject getAsJsonObject() {
		JsonObject ret = new JsonObject();

		for (Map.Entry<BaseEnchantment, Integer> entry : mCurrentProperties.entrySet()) {
			ret.addProperty(ChatColor.stripColor(entry.getKey().getProperty()), entry.getValue());
		}
		return ret;
	}
}
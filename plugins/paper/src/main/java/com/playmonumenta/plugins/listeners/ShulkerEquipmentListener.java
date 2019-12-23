package com.playmonumenta.plugins.listeners;

import java.util.Map;
import java.util.TreeMap;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;

public class ShulkerEquipmentListener implements Listener {
	private static final String LOCK_STRING = "AdminEquipmentTool";
	private static final Map<Integer, Integer> SWAP_SLOTS = new TreeMap<Integer, Integer>();

	static {
		SWAP_SLOTS.put(0, 0);
		SWAP_SLOTS.put(1, 1);
		SWAP_SLOTS.put(2, 2);
		SWAP_SLOTS.put(3, 3);
		SWAP_SLOTS.put(4, 4);
		SWAP_SLOTS.put(5, 5);
		SWAP_SLOTS.put(6, 6);
		SWAP_SLOTS.put(7, 7);
		SWAP_SLOTS.put(8, 8);
		SWAP_SLOTS.put(36, 9);
		SWAP_SLOTS.put(37, 10);
		SWAP_SLOTS.put(38, 11);
		SWAP_SLOTS.put(39, 12);
		SWAP_SLOTS.put(40, 13);
	}

	Plugin mPlugin = null;

	public ShulkerEquipmentListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void InventoryClickEvent(InventoryClickEvent event) {
		if (
		    // Must not be cancelled
		    event.isCancelled() ||
		    // Must be a right click
		    event.getClick() == null ||
		    !event.getClick().equals(ClickType.RIGHT) ||
		    // Must be placing a single block
		    event.getAction() == null ||
		    !event.getAction().equals(InventoryAction.PICKUP_HALF) ||
		    // Must be a player interacting with their main inventory
		    event.getWhoClicked() == null ||
		    !(event.getWhoClicked() instanceof Player) ||
		    event.getClickedInventory() == null ||
		    !(event.getClickedInventory() instanceof PlayerInventory ||
		      event.getClickedInventory().getType().equals(InventoryType.ENDER_CHEST) ||
		      event.getClickedInventory().getType().equals(InventoryType.CHEST)) ||
		    // Must be in main inventory
		    // https://minecraft.gamepedia.com/Player.dat_format#Inventory_slot_numbers
		    event.getSlot() < 9 ||
		    event.getSlot() > 35 ||
		    // Must be a click on a shulker box with an empty hand
		    (event.getCursor() != null && !event.getCursor().getType().equals(Material.AIR)) ||
		    event.getCurrentItem() == null ||
		    !ItemUtils.isShulkerBox(event.getCurrentItem().getType())
		) {

			// Nope!
			return;
		}

		Player player = (Player)event.getWhoClicked();
		PlayerInventory pInv = player.getInventory();
		ItemStack sboxItem = event.getCurrentItem();

		if (ItemUtils.isShulkerBox(sboxItem.getType()) && !ItemUtils.isItemShattered(sboxItem) && sboxItem.hasItemMeta()) {
			if (sboxItem.getItemMeta() instanceof BlockStateMeta) {
				BlockStateMeta sMeta = (BlockStateMeta)sboxItem.getItemMeta();
				if (sMeta.getBlockState() instanceof ShulkerBox) {
					ShulkerBox sbox = (ShulkerBox)sMeta.getBlockState();

					if (sbox.isLocked() && sbox.getLock().equals(LOCK_STRING)) {
						swap(player, pInv, sbox);

						sMeta.setBlockState(sbox);
						sboxItem.setItemMeta(sMeta);

						player.updateInventory();
						event.setCancelled(true);
						InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, event);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void BlockDispenseEvent(BlockDispenseEvent event) {
		if (event.isCancelled()) {
			return;
		}

		if (isEquipmentBox(event.getItem())) {
			event.setCancelled(true);
		}
	}

	public static boolean isEquipmentBox(ItemStack sboxItem) {
		if (sboxItem != null && ItemUtils.isShulkerBox(sboxItem.getType()) && sboxItem.hasItemMeta()) {
			if (sboxItem.getItemMeta() instanceof BlockStateMeta) {
				BlockStateMeta sMeta = (BlockStateMeta)sboxItem.getItemMeta();
				if (sMeta.getBlockState() instanceof ShulkerBox) {
					ShulkerBox sbox = (ShulkerBox)sMeta.getBlockState();

					if (sbox.isLocked() && sbox.getLock().equals(LOCK_STRING)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private void swap(Player player, PlayerInventory pInv, ShulkerBox sbox) {
		/* Prevent swapping/nesting shulkers */
		for (Map.Entry<Integer, Integer> slot : SWAP_SLOTS.entrySet()) {
			ItemStack item = pInv.getItem(slot.getKey());
			if (item != null && ItemUtils.isShulkerBox(item.getType())) {
				player.sendMessage(ChatColor.RED + "You can not store shulker boxes");
				player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.1f);
				return;
			}
		}

		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Equipment Swapped");
		player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.1f);
		Inventory sInv = sbox.getInventory();

		for (Map.Entry<Integer, Integer> slot : SWAP_SLOTS.entrySet()) {
			//Does not swap if armor equipped has curse of binding on it
			if (slot.getKey() < 36 || slot.getKey() > 39 || pInv.getItem(slot.getKey()) == null || pInv.getItem(slot.getKey()).getEnchantmentLevel(Enchantment.BINDING_CURSE) == 0) {
				swapItem(pInv, sInv, slot.getKey(), slot.getValue());
			}
		}
	}

	private void swapItem(Inventory from, Inventory to, int fromSlot, int toSlot) {
		ItemStack tmp = from.getItem(fromSlot);
		from.setItem(fromSlot, to.getItem(toSlot));
		to.setItem(toSlot, tmp);
	}
}

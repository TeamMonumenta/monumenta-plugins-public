package com.playmonumenta.plugins.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
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
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.Stasis;
import com.playmonumenta.plugins.enchantments.Locked;
import com.playmonumenta.plugins.enchantments.curses.CurseOfEphemerality;
import com.playmonumenta.plugins.overrides.FirmamentOverride;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;

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
	private Map<UUID, BukkitRunnable> mCooldowns = new HashMap<UUID, BukkitRunnable>();


	public ShulkerEquipmentListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void inventoryClickEvent(InventoryClickEvent event) {
		String s = "Stasis";
		NavigableSet<Effect> activeEffects = mPlugin.mEffectManager.getEffects(event.getWhoClicked(), s);
		if (activeEffects != null && activeEffects.contains(new Stasis(120))) {
			event.setCancelled(true);
		}
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
		    // If it's a player inventory, must be in main inventory
		    // https://minecraft.gamepedia.com/Player.dat_format#Inventory_slot_numbers
			(event.getClickedInventory() instanceof PlayerInventory && (event.getSlot() < 9 || event.getSlot() > 35)) ||
			// Must be a player inventory, ender chest, or regular chest
		    !(event.getClickedInventory() instanceof PlayerInventory ||
		      event.getClickedInventory().getType().equals(InventoryType.ENDER_CHEST) ||
		      event.getClickedInventory().getType().equals(InventoryType.CHEST)) ||
		    // Must be a click on a shulker box with an empty hand
		    (event.getCursor() != null && !event.getCursor().getType().equals(Material.AIR)) ||
		    event.getCurrentItem() == null ||
		    !ItemUtils.isShulkerBox(event.getCurrentItem().getType())
		) {

			// Nope!
			return;
		}

		Player player = (Player)event.getWhoClicked();

		if (ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.NO_PORTABLE_STORAGE)) {
			player.sendMessage(ChatColor.RED + "You can't use this here");
			player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, SoundCategory.PLAYERS, 1.0f, 0.6f);
			return;
		}

		PlayerInventory pInv = player.getInventory();
		ItemStack sboxItem = event.getCurrentItem();

		if (ItemUtils.isShulkerBox(sboxItem.getType()) && !ItemUtils.isItemShattered(sboxItem) && sboxItem.hasItemMeta()) {
			if (sboxItem.getItemMeta() instanceof BlockStateMeta) {
				BlockStateMeta sMeta = (BlockStateMeta)sboxItem.getItemMeta();
				if (sMeta.getBlockState() instanceof ShulkerBox) {
					ShulkerBox sbox = (ShulkerBox)sMeta.getBlockState();

					if (sbox.isLocked() && sbox.getLock().equals(LOCK_STRING)) {

						//if on cooldown don't swap
						if (checkSwapCooldown(player)) {
				            player.sendMessage(ChatColor.RED + "Lockbox still on cooldown!");
				            player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
				            event.setCancelled(true);
				            return;
						}

						swap(player, pInv, sbox);

						//check if swapped in radius of boss
						Location loc = player.getLocation();
						for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, 24)) {
					        if (mob.getScoreboardTags().contains("Boss")) {
					            player.sendMessage(ChatColor.RED + "Close to boss - Lockbox on 15s cooldown!");
					            setSwapCooldown(player);
					        }
					    }

						sMeta.setBlockState(sbox);
						sboxItem.setItemMeta(sMeta);
						player.updateInventory();
						event.setCancelled(true);
						InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, null);

					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void blockDispenseEvent(BlockDispenseEvent event) {
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
			if (item != null && ItemUtils.isShulkerBox(item.getType()) && !FirmamentOverride.isFirmamentItem(item)) {
				player.sendMessage(ChatColor.RED + "You can not store shulker boxes");
				player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.1f);
				return;
			}
		}

		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Equipment Swapped");
		player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.1f);
		Inventory sInv = sbox.getInventory();

		for (Map.Entry<Integer, Integer> slot : SWAP_SLOTS.entrySet()) {
			if (slot.getKey() >= 36 && slot.getKey() <= 39 && pInv.getItem(slot.getKey()) != null && pInv.getItem(slot.getKey()).getEnchantmentLevel(Enchantment.BINDING_CURSE) != 0) {
				//Does not swap if armor equipped has curse of binding on it
			} else if (pInv.getItem(slot.getKey()) != null && InventoryUtils.getCustomEnchantLevel(pInv.getItem(slot.getKey()), CurseOfEphemerality.PROPERTY_NAME, false) != 0) {
				//Doesn't swap with curse of ephemerality either
			} else if (pInv.getItem(slot.getKey()) != null && InventoryUtils.getCustomEnchantLevel(pInv.getItem(slot.getKey()), Locked.PROPERTY_NAME, false) != 0) {
				//Doesn't swap with Locked either
			} else {
				swapItem(pInv, sInv, slot.getKey(), slot.getValue());
			}
		}
	}

	private void swapItem(Inventory from, Inventory to, int fromSlot, int toSlot) {
		ItemStack tmp = from.getItem(fromSlot);
		from.setItem(fromSlot, to.getItem(toSlot));
		to.setItem(toSlot, tmp);
	}

	//Set cooldown after swapping in RADIUS 24 blocks of boss
	private void setSwapCooldown(Player player) {
		BukkitRunnable runnable = mCooldowns.remove(player.getUniqueId());
		if (runnable != null) {
			runnable.cancel();
		}
		runnable = new BukkitRunnable() {
			@Override
			public void run() {
				//Don't need to put anything here - method checks if BukkitRunnable is active
				mCooldowns.remove(player.getUniqueId());
			}
		};
		runnable.runTaskLater(mPlugin, 20 * 15); //15s cooldown
		mCooldowns.put(player.getUniqueId(), runnable);
	}

	//Returns true if cooldown is up right now
	//False if no cooldowns and the lockbox is activatable now
	private boolean checkSwapCooldown(Player player) {
		return mCooldowns.containsKey(player.getUniqueId()) && !mCooldowns.get(player.getUniqueId()).isCancelled();
	}
}

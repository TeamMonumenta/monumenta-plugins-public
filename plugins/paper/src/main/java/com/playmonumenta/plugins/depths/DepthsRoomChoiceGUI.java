package com.playmonumenta.plugins.depths;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.goncalomb.bukkit.mylib.utils.CustomInventory;

public class DepthsRoomChoiceGUI extends CustomInventory {
	private static final Material FILLER = Material.BLACK_STAINED_GLASS_PANE;

	public DepthsRoomChoiceGUI(Player player) {
		super(player, 27, "Select the Next Room Type");

		EnumSet<DepthsRoomType> roomChoices = DepthsManager.getInstance().generateRoomOptions(player);

		if (roomChoices.contains(DepthsRoomType.ABILITY)) {
			ItemStack stack = new ItemStack(Material.BOOK, 1);
			ItemMeta meta = stack.getItemMeta();
			meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Normal Room with Ability Reward");
			stack.setItemMeta(meta);
			_inventory.setItem(0, stack);
		} else {
			_inventory.setItem(0, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
		}

		if (roomChoices.contains(DepthsRoomType.ABILITY_ELITE)) {
			ItemStack stack = new ItemStack(Material.ENCHANTED_BOOK, 1);
			ItemMeta meta = stack.getItemMeta();
			meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Elite Room with Ability Reward");
			stack.setItemMeta(meta);
			_inventory.setItem(18, stack);
		} else {
			_inventory.setItem(18, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
		}

		if (roomChoices.contains(DepthsRoomType.BOSS)) {
			ItemStack stack = new ItemStack(Material.WITHER_ROSE, 1);
			ItemMeta meta = stack.getItemMeta();
			meta.setDisplayName(ChatColor.RED + "Boss Challenge");
			stack.setItemMeta(meta);
			_inventory.setItem(22, stack);
		} else {
			_inventory.setItem(22, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
		}

		if (roomChoices.contains(DepthsRoomType.UPGRADE)) {
			ItemStack stack = new ItemStack(Material.DAMAGED_ANVIL, 1);
			ItemMeta meta = stack.getItemMeta();
			meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Normal Room with Upgrade Reward");
			stack.setItemMeta(meta);
			_inventory.setItem(8, stack);
		} else {
			_inventory.setItem(8, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
		}

		if (roomChoices.contains(DepthsRoomType.UPGRADE_ELITE)) {
			ItemStack stack = new ItemStack(Material.ANVIL, 1);
			ItemMeta meta = stack.getItemMeta();
			meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Elite Room with Upgrade Reward");
			stack.setItemMeta(meta);
			_inventory.setItem(26, stack);
		} else {
			_inventory.setItem(26, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
		}

		if (roomChoices.contains(DepthsRoomType.TREASURE)) {
			ItemStack stack = new ItemStack(Material.GOLD_NUGGET, 1);
			ItemMeta meta = stack.getItemMeta();
			meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Normal Room with Treasure Reward");
			stack.setItemMeta(meta);
			_inventory.setItem(11, stack);
		} else {
			_inventory.setItem(11, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
		}

		if (roomChoices.contains(DepthsRoomType.TREASURE_ELITE)) {
			ItemStack stack = new ItemStack(Material.GOLD_INGOT, 1);
			ItemMeta meta = stack.getItemMeta();
			meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Elite Room with Treasure Reward");
			stack.setItemMeta(meta);
			_inventory.setItem(15, stack);
		} else {
			_inventory.setItem(15, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
		}

		if (roomChoices.contains(DepthsRoomType.UTILITY)) {
			ItemStack stack = new ItemStack(Material.ENDER_CHEST, 1);
			ItemMeta meta = stack.getItemMeta();
			meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Utility Room");
			stack.setItemMeta(meta);
			_inventory.setItem(4, stack);
		} else {
			_inventory.setItem(4, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
		}
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (event.getClickedInventory() != _inventory ||
				event.getCurrentItem().getType() == FILLER ||
				event.isShiftClick()) {
			return;
		}

		Player p = (Player) event.getWhoClicked();

		if (event.getSlot() == 0) {
			DepthsManager.getInstance().playerSelectedRoom(DepthsRoomType.ABILITY, p);
		} else if (event.getSlot() == 18) {
			DepthsManager.getInstance().playerSelectedRoom(DepthsRoomType.ABILITY_ELITE, p);
		} else if (event.getSlot() == 22) {
			DepthsManager.getInstance().playerSelectedRoom(DepthsRoomType.BOSS, p);
		} else if (event.getSlot() == 8) {
			DepthsManager.getInstance().playerSelectedRoom(DepthsRoomType.UPGRADE, p);
		} else if (event.getSlot() == 26) {
			DepthsManager.getInstance().playerSelectedRoom(DepthsRoomType.UPGRADE_ELITE, p);
		} else if (event.getSlot() == 11) {
			DepthsManager.getInstance().playerSelectedRoom(DepthsRoomType.TREASURE, p);
		} else if (event.getSlot() == 15) {
			DepthsManager.getInstance().playerSelectedRoom(DepthsRoomType.TREASURE_ELITE, p);
		} else if (event.getSlot() == 4) {
			DepthsManager.getInstance().playerSelectedRoom(DepthsRoomType.UTILITY, p);
		}

		event.getWhoClicked().closeInventory();
	}
}

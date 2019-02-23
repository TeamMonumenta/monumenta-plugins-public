package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.InventoryUtils;

/*
 * Multitool - Level one allows you to swap the tool
 * between an axe and shovel, level 2 adds a pickaxe
 * to the rotation. Swapping tools keeps the same
 * Name/Lore/Stats.
 */
public class Multitool implements BaseEnchantment {
	private static String PROPERTY_NAME = ChatColor.GRAY + "Multitool";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onPlayerInteract(Plugin plugin, Player player, PlayerInteractEvent event, int level) {
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			ItemStack item = player.getInventory().getItemInMainHand();
			String[] str = item.getType().toString().split("_");
			if (InventoryUtils.isAxeItem(item)) {
				Material mat = Material.valueOf(str[0] + "_" + "SHOVEL");
				item.setType(mat);
			} else if (InventoryUtils.isShovelItem(item)) {
				if (level > 1) {
					Material mat = Material.valueOf(str[0] + "_" + "PICKAXE");
					item.setType(mat);
				} else {
					Material mat = Material.valueOf(str[0] + "_" + "AXE");
					item.setType(mat);
				}
			} else if (InventoryUtils.isPickaxeItem(item)) {
				Material mat = Material.valueOf(str[0] + "_" + "AXE");
				item.setType(mat);
			}
			player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1, 2F);
		}
	}

}
